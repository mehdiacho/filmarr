from fastapi import APIRouter, Depends
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.auth.deps import get_current_user
from app.auth.models import User
from app.db.session import get_db
from app.letterboxd.csv_adapter import CSVAdapter

router = APIRouter(prefix="/letterboxd", tags=["letterboxd"])


@router.get("/export")
async def export_csv(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    adapter = CSVAdapter(db)
    csv_bytes = await adapter.export(user_id=current_user.id)
    return Response(
        content=csv_bytes,
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=filmarr-export.csv"},
    )
