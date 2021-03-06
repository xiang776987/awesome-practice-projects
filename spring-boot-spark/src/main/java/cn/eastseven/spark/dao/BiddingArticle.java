package cn.eastseven.spark.dao;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

/**
 * @author eastseven
 */
@Data
//@Document(collection = "bidding_article")
public class BiddingArticle implements Serializable {

    @Id
    private String rowKey;

    private String url;

    private String title;

    private String industry;

    private String code;
}
