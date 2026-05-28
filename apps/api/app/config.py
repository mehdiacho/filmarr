from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql+asyncpg://postgres:postgres@localhost:5432/filmarr"
    secret_key: str = "change-me"
    access_token_expire_minutes: int = 10080  # 1 week
    tmdb_api_key: str = ""
    tmdb_base_url: str = "https://api.themoviedb.org/3"
    # Supabase Storage (avatar uploads). Service key set via Render env.
    supabase_url: str = "https://itpfwheuycbnegvszqjf.supabase.co"
    supabase_service_key: str = ""


settings = Settings()
