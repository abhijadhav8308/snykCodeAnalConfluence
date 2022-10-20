package com.miniorange.exporter.confluence;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MoConfluenceComment {

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter dateFormatterNewNot = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy HH:mm:ss a");
    private static final DateTimeFormatter dateFormatterNew = DateTimeFormatter.ofPattern("EEE, d MMM, hh:mm a");
    private final Logger LOGGER = (Logger) LoggerFactory.getLogger(this.getClass());
    private final String commentBody;
    private final String author;
    private final String commentDate;
    private final String authorThumbnailPath;

    private final String commentAuthorAccountId;


    public MoConfluenceComment(String author, String commentDate, String commentBody, String authorThumbnailPath, String commentAuthorAccountId) {
        this.commentBody = commentBody;
        this.author = author;
        this.commentDate = dateFormatterNew.format(LocalDateTime.parse(commentDate.replace("T", " ").replace("Z", ""), dateFormatter));
        this.authorThumbnailPath = authorThumbnailPath;
        this.commentAuthorAccountId = commentAuthorAccountId;
    }

    public String getAuthorThumbnailPath() {
        return authorThumbnailPath;
    }

    public String getCommentBody() {
        return commentBody;
    }

    public String getAuthor() {
        return author;
    }


    public String getCommentDate() {
        return commentDate;
    }

    public String getCommentAuthorAccountId() {
        return commentAuthorAccountId;
    }
}
