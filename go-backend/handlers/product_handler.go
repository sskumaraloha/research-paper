package handlers

import (
	"database/sql"
	"errors"
	"net/http"
	"strconv"
	"strings"

	"github.com/labstack/echo/v4"

	"go-backend/models"
)

// ProductHandler holds the database handle used by the product endpoints.
type ProductHandler struct {
	DB *sql.DB
}

// NewProductHandler creates a ProductHandler backed by the given database.
func NewProductHandler(db *sql.DB) *ProductHandler {
	return &ProductHandler{DB: db}
}

// Create handles POST /api/products.
func (h *ProductHandler) Create(c echo.Context) error {
	var req models.CreateProductRequest
	if err := c.Bind(&req); err != nil {
		return echo.NewHTTPError(http.StatusBadRequest, "invalid request body")
	}
	if err := validateProduct(req.Name, req.Price); err != nil {
		return err
	}

	var p models.Product
	err := h.DB.QueryRowContext(c.Request().Context(),
		`INSERT INTO products (name, description, price)
		 VALUES ($1, $2, $3)
		 RETURNING id, name, description, price, created_at, updated_at`,
		strings.TrimSpace(req.Name), req.Description, req.Price,
	).Scan(&p.ID, &p.Name, &p.Description, &p.Price, &p.CreatedAt, &p.UpdatedAt)
	if err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, "failed to create product")
	}

	return c.JSON(http.StatusCreated, p)
}

// List handles GET /api/products.
func (h *ProductHandler) List(c echo.Context) error {
	rows, err := h.DB.QueryContext(c.Request().Context(),
		`SELECT id, name, description, price, created_at, updated_at
		 FROM products ORDER BY id`)
	if err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, "failed to list products")
	}
	defer rows.Close()

	products := []models.Product{}
	for rows.Next() {
		var p models.Product
		if err := rows.Scan(&p.ID, &p.Name, &p.Description, &p.Price, &p.CreatedAt, &p.UpdatedAt); err != nil {
			return echo.NewHTTPError(http.StatusInternalServerError, "failed to read products")
		}
		products = append(products, p)
	}
	if err := rows.Err(); err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, "failed to read products")
	}

	return c.JSON(http.StatusOK, products)
}

// Get handles GET /api/products/:id.
func (h *ProductHandler) Get(c echo.Context) error {
	id, err := parseID(c)
	if err != nil {
		return err
	}

	var p models.Product
	err = h.DB.QueryRowContext(c.Request().Context(),
		`SELECT id, name, description, price, created_at, updated_at
		 FROM products WHERE id = $1`, id,
	).Scan(&p.ID, &p.Name, &p.Description, &p.Price, &p.CreatedAt, &p.UpdatedAt)
	if errors.Is(err, sql.ErrNoRows) {
		return echo.NewHTTPError(http.StatusNotFound, "product not found")
	}
	if err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, "failed to get product")
	}

	return c.JSON(http.StatusOK, p)
}

// Update handles PUT /api/products/:id.
func (h *ProductHandler) Update(c echo.Context) error {
	id, err := parseID(c)
	if err != nil {
		return err
	}

	var req models.UpdateProductRequest
	if err := c.Bind(&req); err != nil {
		return echo.NewHTTPError(http.StatusBadRequest, "invalid request body")
	}
	if err := validateProduct(req.Name, req.Price); err != nil {
		return err
	}

	var p models.Product
	err = h.DB.QueryRowContext(c.Request().Context(),
		`UPDATE products
		 SET name = $1, description = $2, price = $3, updated_at = NOW()
		 WHERE id = $4
		 RETURNING id, name, description, price, created_at, updated_at`,
		strings.TrimSpace(req.Name), req.Description, req.Price, id,
	).Scan(&p.ID, &p.Name, &p.Description, &p.Price, &p.CreatedAt, &p.UpdatedAt)
	if errors.Is(err, sql.ErrNoRows) {
		return echo.NewHTTPError(http.StatusNotFound, "product not found")
	}
	if err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, "failed to update product")
	}

	return c.JSON(http.StatusOK, p)
}

// Delete handles DELETE /api/products/:id.
func (h *ProductHandler) Delete(c echo.Context) error {
	id, err := parseID(c)
	if err != nil {
		return err
	}

	res, err := h.DB.ExecContext(c.Request().Context(),
		`DELETE FROM products WHERE id = $1`, id)
	if err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, "failed to delete product")
	}
	affected, err := res.RowsAffected()
	if err != nil {
		return echo.NewHTTPError(http.StatusInternalServerError, "failed to delete product")
	}
	if affected == 0 {
		return echo.NewHTTPError(http.StatusNotFound, "product not found")
	}

	return c.NoContent(http.StatusNoContent)
}

func parseID(c echo.Context) (int64, error) {
	id, err := strconv.ParseInt(c.Param("id"), 10, 64)
	if err != nil || id <= 0 {
		return 0, echo.NewHTTPError(http.StatusBadRequest, "id must be a positive integer")
	}
	return id, nil
}

func validateProduct(name string, price float64) error {
	if strings.TrimSpace(name) == "" {
		return echo.NewHTTPError(http.StatusBadRequest, "name is required")
	}
	if price < 0 {
		return echo.NewHTTPError(http.StatusBadRequest, "price must be zero or greater")
	}
	return nil
}
