package main

import (
	"net/http"
	"os"

	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"

	"go-backend/db"
	"go-backend/handlers"
)

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
