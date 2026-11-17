INSERT INTO track_logs(manga_id,chapters,created_at,unread) SELECT manga_id,'Ch. 201 - The Promise'||char(10)||'Ch. 202 - Morning After',(strftime('%s','now')-1800)*1000,1 FROM (SELECT DISTINCT manga_id FROM favourites LIMIT 4);
INSERT INTO track_logs(manga_id,chapters,created_at,unread) SELECT manga_id,'Vol. 9 Ch. 77',(strftime('%s','now')-90000)*1000,0 FROM (SELECT DISTINCT manga_id FROM favourites LIMIT 4 OFFSET 4);
INSERT INTO track_logs(manga_id,chapters,created_at,unread) SELECT manga_id,'Ch. 55'||char(10)||'Ch. 56'||char(10)||'Ch. 57',(strftime('%s','now')-260000)*1000,1 FROM (SELECT DISTINCT manga_id FROM favourites LIMIT 3 OFFSET 8);
INSERT OR REPLACE INTO tracks(manga_id,last_chapter_id,chapters_new,last_check_time,last_chapter_date,last_result,last_error) SELECT manga_id,0,2,strftime('%s','now')*1000,strftime('%s','now')*1000,1,NULL FROM (SELECT DISTINCT manga_id FROM favourites LIMIT 4);
select count(*) from track_logs;
