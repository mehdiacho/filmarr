from pydantic import BaseModel, EmailStr


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class OnboardingRequest(BaseModel):
    genres: list[str]


class UserOut(BaseModel):
    id: int
    email: str
    preferences: dict = {}

    model_config = {"from_attributes": True}
