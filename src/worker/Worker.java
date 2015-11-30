package worker;

import static tweetBasic.AWSResourceSetup.SQS_QUEUE_NAME;
import static tweetBasic.AWSResourceSetup.SQS;
import static tweetBasic.AWSResourceSetup.DYNAMODB_MAPPER;
import static tweetBasic.AWSResourceSetup.SNS_TOPIC_ARN;
import static tweetBasic.AWSResourceSetup.SNS;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tweetBasic.Tweet;

import com.alchemyapi.api.AlchemyAPI;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;


public class Worker extends Thread {
	//thread pool with size 10
	
    static AlchemyAPI alchemyObj;
	
	static{
		try {
			alchemyObj = AlchemyAPI.GetInstanceFromFile("api_key.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(10);
    
    public static void main(String[] args) throws InterruptedException {
        new Worker().start();
    }
    
    @Override
    public void run() {
        System.out.println("[Worker] Worker listening for work");
        String queueUrl = SQS.getQueueUrl(new GetQueueUrlRequest(SQS_QUEUE_NAME)).getQueueUrl();

        while (true) {
            try {
                ReceiveMessageResult result = SQS.receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(1));
                for (Message msg : result.getMessages()) {
                	// Put into the working thread pool
                    executorService.submit(new MessageProcessor(queueUrl, msg));
                }
                sleep(1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new RuntimeException("Worker interrupted");
            } catch (Exception e) {
                // ignore and retry
            }
        }
    }
    
    private static String getSentiment(Document doc) {
    	Element parent = (Element) doc.getElementsByTagName("docSentiment").item(0);
    	NodeList childList = parent.getElementsByTagName("type");
    	String sentiment = childList.item(0).getTextContent();
    	return sentiment;
    }
    
    private static String getCategory(Document doc) {
    	String category = doc.getElementsByTagName("category").item(0).getTextContent();
    	return category;
    }
    
    //A runnable task
    private final class MessageProcessor implements Runnable {
        private final String queueUrl;
        private final Message msg;

        private MessageProcessor(String queueUrl, Message msg) {
            this.queueUrl = queueUrl;
            this.msg = msg;
        }

        @Override
        public void run() {
            String id = msg.getBody();
            System.out.println("[Worker] MSG: "+msg.getBody()+" thread ID: "+Thread.currentThread().getId());
            Tweet tweet=null;
			try {
				tweet = DYNAMODB_MAPPER.load(Tweet.class, id);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

            try {
            	
            	if (tweet == null) {
            		System.out.println("[Worker] tweet is null");
            	} else {
                	System.out.println("[Worker] tweet not null");
                	// Process Content, save back to db
                	// Extract sentiment for a text string.
                    Document doc;
                    String category = "";
                    String sentiment = "";
    				try {
    					String text = tweet.getContent();
    					doc = alchemyObj.TextGetTextSentiment(text);
    					sentiment = getSentiment(doc);
    					doc = alchemyObj.TextGetCategory(text);
    					category = getCategory(doc);
    				} catch (XPathExpressionException e) {
    				} catch (IOException e) {
    				} catch (SAXException e) {
    				} catch (ParserConfigurationException e) {
    				}

					// Log tweet details. User - Date - (Lat, Long) - Content - Sentiment - Category.
                    System.out.println(tweet.getUsername()
                    		+ " on " + tweet.getCreatedDate()
                    		+ " at (" + tweet.getGeoLat() + ", " + tweet.getGeoLng() + ")\n"
                    		+ tweet.getContent());
                    
                    if (sentiment.length() != 0 && category.length() != 0) {
                    	tweet.setCategory(category);
                    	tweet.setSentiment(sentiment);
                    	System.out.println("[Worker] sentiment: " + sentiment + "; " + "category: " + category);
                        System.out.println("[Worker] save tweet");
                        DYNAMODB_MAPPER.save(tweet);
                        
                        // Publish a notification to indicate a new tweet is processed
                        PublishRequest publishReq = new PublishRequest()
                        .withTopicArn(SNS_TOPIC_ARN)
                        .withMessage(tweet.getId());
                        SNS.publish(publishReq);
                        System.out.println("[Worker] publish notification");
                        
                    } else {
                    	tweet.setCategory("uncategorizable");
                    	tweet.setSentiment("unsentimental");
                    	System.out.println("[Worker] no sentiment; no category");
                    	// System.out.println("[Worker] delete tweet " + id);
                        // DYNAMODB_MAPPER.delete(tweet);
                    }
                    
                    Thread.sleep(1000);                  
                }
            } catch (Exception e) {
                e.printStackTrace();
                
            }
            
            System.out.println("[Worker] delete queue url");
            SQS.deleteMessage(new DeleteMessageRequest(queueUrl, msg.getReceiptHandle()));
        }
        
        
        
    }
}
