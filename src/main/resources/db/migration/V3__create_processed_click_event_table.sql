CREATE TABLE processed_click_event (
    event_id UUID PRIMARY KEY,
    short_code VARCHAR(8) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
)