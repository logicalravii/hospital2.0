-- W3C trace context (traceparent header value, 55 chars) captured when the outbox
-- row is written, so the eventual Kafka publish joins the originating request's trace.
-- Nullable: rows written outside a trace, and rows created before this migration, have none.
ALTER TABLE outbox_event ADD COLUMN trace_parent VARCHAR(55);
