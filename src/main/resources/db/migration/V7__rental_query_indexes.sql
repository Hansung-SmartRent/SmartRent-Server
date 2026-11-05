CREATE INDEX idx_rentals_user_status ON rentals(user_id,status,created_at);
CREATE INDEX idx_rentals_status_start ON rentals(status,start_at);
CREATE INDEX idx_rentals_status_end ON rentals(status,end_at);
CREATE INDEX idx_rentals_unit_status ON rentals(unit_id,status);
