package models

import "time"

// Product represents a product record in the database.
type Product struct {
	ID          int64     `json:"id"`
	Name        string    `json:"name"`
	Description string    `json:"description"`
	Price       float64   `json:"price"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
}

// CreateProductRequest is the payload for creating a product.
type CreateProductRequest struct {
	Name        string  `json:"name" validate:"required"`
	Description string  `json:"description"`
	Price       float64 `json:"price" validate:"gte=0"`
}

// UpdateProductRequest is the payload for updating a product.
type UpdateProductRequest struct {
	Name        string  `json:"name" validate:"required"`
	Description string  `json:"description"`
	Price       float64 `json:"price" validate:"gte=0"`
}

// ErrorResponse is the JSON error body returned by the API.
type ErrorResponse struct {
	Message string `json:"message" example:"product not found"`
}
