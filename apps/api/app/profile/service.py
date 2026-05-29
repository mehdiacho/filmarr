"""Profile helpers: favorites resolution, statistics, and follow relationships."""
from collections import Counter

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.models import User
from app.library.models import Interaction
from app.movies.models import Movie
from app.profile import schemas
from app.social.models import Follow


async def resolve_favorites(db: AsyncSession, tmdb_ids: list[int]) -> list[Movie]:
    if not tmdb_ids:
        return []
    result = await db.execute(select(Movie).where(Movie.tmdb_id.in_(tmdb_ids)))
    by_tmdb = {m.tmdb_id: m for m in result.scalars().all()}
    return [by_tmdb[i] for i in tmdb_ids if i in by_tmdb]  # preserve order


async def follower_count(db: AsyncSession, user_id: int) -> int:
    r = await db.execute(
        select(func.count()).select_from(Follow).where(Follow.followee_id == user_id)
    )
    return r.scalar() or 0


async def following_count(db: AsyncSession, user_id: int) -> int:
    r = await db.execute(
        select(func.count()).select_from(Follow).where(Follow.follower_id == user_id)
    )
    return r.scalar() or 0


async def following_ids(db: AsyncSession, follower_id: int) -> set[int]:
    r = await db.execute(
        select(Follow.followee_id).where(Follow.follower_id == follower_id)
    )
    return set(r.scalars().all())


async def build_user_cards(
    db: AsyncSession, users: list[User], viewer_id: int
) -> list[schemas.UserCard]:
    follows = await following_ids(db, viewer_id)
    return [
        schemas.UserCard(
            id=u.id,
            username=u.username,
            display_name=u.display_name,
            avatar_url=u.avatar_url,
            is_following=u.id in follows,
        )
        for u in users
    ]


async def compute_stats(db: AsyncSession, user_id: int) -> schemas.ProfileStats:
    result = await db.execute(
        select(Interaction, Movie)
        .join(Movie, Movie.id == Interaction.movie_id)
        .where(Interaction.user_id == user_id, Interaction.direction == "right")
    )
    rows = result.all()

    watched_movie_ids: set[int] = set()
    ratings: list[float] = []
    reviews = 0
    genre_counter: Counter[str] = Counter()
    decade_counter: Counter[int] = Counter()

    for interaction, movie in rows:
        first_time = movie.id not in watched_movie_ids
        watched_movie_ids.add(movie.id)
        if interaction.rating is not None:
            ratings.append(interaction.rating)
        if interaction.review:
            reviews += 1
        if first_time:
            for g in movie.genres or []:
                genre_counter[g] += 1
            if movie.year:
                decade_counter[(movie.year // 10) * 10] += 1

    histogram = {f"{half / 2:.1f}": 0 for half in range(1, 11)}  # 0.5..5.0
    for r in ratings:
        key = f"{r:.1f}"
        if key in histogram:
            histogram[key] += 1

    avg = round(sum(ratings) / len(ratings), 2) if ratings else None

    top_genres = [
        schemas.GenreCount(name=g, count=c) for g, c in genre_counter.most_common(6)
    ]
    top_decades = [
        schemas.DecadeCount(decade=d, count=c)
        for d, c in sorted(decade_counter.items(), key=lambda x: x[1], reverse=True)[:6]
    ]

    return schemas.ProfileStats(
        films_watched=len(watched_movie_ids),
        reviews=reviews,
        avg_rating=avg,
        rating_histogram=histogram,
        top_genres=top_genres,
        top_decades=top_decades,
    )
