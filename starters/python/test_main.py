from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def test_health():
    response = client.get("/health")

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert data["service"] == "SERVICE_NAME"


def test_health_content_type():
    response = client.get("/health")

    assert response.headers["content-type"] == "application/json"
