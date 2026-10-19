-- M8-DEP-002. Apply explicitly to the configured development database before deployment.
-- Parent tables users and publications must already exist. No existing data is modified.
CREATE TABLE IF NOT EXISTS product_reviews (
    id UUID PRIMARY KEY,
    publication_id UUID NOT NULL REFERENCES publications(id),
    buyer_id UUID NOT NULL REFERENCES users(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(1000) NOT NULL CHECK (CHAR_LENGTH(TRIM(comment)) BETWEEN 1 AND 1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_reviews_publication_created ON product_reviews(publication_id, created_at, id);
