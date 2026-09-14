package main

import (
	"net/http"
	"os"

	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	echoSwagger "github.com/swaggo/echo-swagger"

	"go-backend/db"
	_ "go-backend/docs"
	"go-backend/handlers"
)

//	@title			Products CRUD API
//	@version		1.0
//	@description	A simple CRUD backend built with Echo and PostgreSQL.

//	@host		localhost:8080
//	@BasePath	/api

//	@tag.name			products
//	@tag.description	Operations on products

func main() {
	e := echo.New()
	e.Use(middleware.Logger())
	e.Use(middleware.Recover())
	e.Use(middleware.CORS())

	conn, err := db.Connect()
	if err != nil {
		e.Logger.Fatalf("database connection failed: %v", err)
	}
	defer conn.Close()

	e.GET("/health", func(c echo.Context) error {
		if err := conn.Ping(); err != nil {
			return c.JSON(http.StatusServiceUnavailable, map[string]string{"status": "down"})
		}
		return c.JSON(http.StatusOK, map[string]string{"status": "ok"})
	})

	e.GET("/swagger/*", echoSwagger.WrapHandler)

	ph := handlers.NewProductHandler(conn)
	api := e.Group("/api")
	api.POST("/products", ph.Create)
	api.GET("/products", ph.List)
	api.GET("/products/:id", ph.Get)
	api.PUT("/products/:id", ph.Update)
	api.DELETE("/products/:id", ph.Delete)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	e.Logger.Fatal(e.Start(":" + port))
}
