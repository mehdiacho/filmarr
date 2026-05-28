from fastapi import APIRouter, Depends
from fastapi.responses import Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db
from app.letterboxd.csv_adapter import CSVAdapter

router = APIRouter(prefix="/letterboxd", tags=["letterboxd"])


@router.get("/export")
async def export_csv(db: AsyncSession = Depends(get_db)):
    adapter = CSVAdapter(db)
    csv_bytes = await adapter.export(user_id=1)
    return Response(
        content=csv_bytes,
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=filmarr-export.csv"},
    )
