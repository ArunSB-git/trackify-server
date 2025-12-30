
---

# 📕 Trackify - Backend (Server)

```md
# 🚀 Trackify – Backend (Server)

This repository contains the **backend API** for Trackify, built using **Spring Boot**.  
It provides authentication, session management, and REST APIs for habit tracking.

---

## 📑 API Reference

- **Swagger UI URL:** https://trackify-server-ireb.onrender.com/swagger-ui/index.html

## 🌐 Live API

- **Base URL:** https://trackify-server-ireb.onrender.com

---

## ✨ Features

- Google OAuth 2.0 authentication
- Session-based login using cookies
- Secure CORS configuration
- Habit & task management APIs
- Monthly completion tracking and metrics
- Production-ready security configuratio

---

## 🛠 Tech Stack

- Java 21
- Spring Boot
- Spring Security
- OAuth2 Client (Google)
- REST APIs
- Maven
- Postgresql

---

## 🔐 Authentication & Security

- OAuth2 login with Google
- HttpSession-based authentication
- Secure cookies with:
  - `SameSite=None`
  - `Secure=true`
- CORS restricted to frontend origin

---

## ⚙️ Environment Variables

Set these in Render or your local environment:

```env
FRONTEND_URL=https://trackify-client-wy2i.onrender.com
