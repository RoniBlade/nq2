import pytest
import requests

BASE_URL = "http://localhost:8080/users"

@pytest.fixture
def new_user():
    return {
        "username": "testuser",
        "password": "password123"
    }

@pytest.fixture
def updated_user():
    return {
        "username": "updateduser",
        "password": "newpassword123"
    }

def test_create_user(new_user):
    response = requests.post(f"{BASE_URL}/register", json=new_user)
    assert response.status_code == 201
    assert response.json()['username'] == new_user['username']

def test_edit_user(new_user, updated_user):
    # Create user
    response = requests.post(f"{BASE_URL}/register", json=new_user)
    user_id = response.json()['id']

    # Edit user
    response = requests.put(f"{BASE_URL}/{user_id}", json=updated_user)
    assert response.status_code == 200
    assert response.json()['username'] == updated_user['username']

def test_change_password(new_user):
    # Create user
    response = requests.post(f"{BASE_URL}/register", json=new_user)
    user_id = response.json()['id']

    # Change password
    new_password = "newpassword123"
    response = requests.put(f"{BASE_URL}/{user_id}/change-password", json=new_password)
    assert response.status_code == 200

def test_change_username(new_user):
    # Create user
    response = requests.post(f"{BASE_URL}/register", json=new_user)
    user_id = response.json()['id']

    # Change username
    new_username = "newusername"
    response = requests.put(f"{BASE_URL}/{user_id}/change-username", json=new_username)
    assert response.status_code == 200
    assert response.json()['username'] == new_username

def test_block_user(new_user):
    # Create user
    response = requests.post(f"{BASE_URL}/register", json=new_user)
    user_id = response.json()['id']

    # Block user
    response = requests.put(f"{BASE_URL}/{user_id}/block")
    assert response.status_code == 200

def test_get_user_by_id(new_user):
    # Create user
    response = requests.post(f"{BASE_URL}/register", json=new_user)
    user_id = response.json()['id']

    # Get user by id
    response = requests.get(f"{BASE_URL}/{user_id}")
    assert response.status_code == 200
    assert response.json()['username'] == new_user['username']