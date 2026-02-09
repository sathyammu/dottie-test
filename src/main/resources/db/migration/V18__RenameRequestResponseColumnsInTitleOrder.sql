alter table title_order rename column genie_request to fees_request;
alter table title_order rename column genie_response to fees_response;
alter table title_order add column order_request text;
alter table title_order add column order_response text;