

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Email implements RequestHandler<SNSEvent, Object> {

	private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
	private static DynamoDB dynamoDB = new DynamoDB(client);

	static String tableName = "billTable"; // System.getenv("databasename");
	static String domainName = "prod.prernasharma.me"; //System.getenv("domainName");

	public Object handleRequest(SNSEvent request, Context context) {
		
		try {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
			context.getLogger().log("Invocation started: " + timeStamp);
			context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());
			ObjectMapper objMap = new ObjectMapper();
			// convert json message to object
			NotificationMessage msg = objMap.readValue(request.getRecords().get(0).getSNS().getMessage(),
					NotificationMessage.class);
	
			Table table = dynamoDB.getTable(tableName);
		
			Item fetchItem = table.getItem("dbId", msg.getUserId());
			if (fetchItem == null) {
				Item item = new Item().withPrimaryKey("dbId", "120").with("TTL", getExpiryTime(context));
				table.putItem(item);
				
				String FROM = "no-reply@"+domainName;

				  String TO = msg.getEmailId() ;

				  String SUBJECT = "Amazon SES test (list of bills)";

				  String TEXTBODY = "This email was sent through Amazon SES "
				      + "using the AWS SDK for Java.";
				List<String> billIDs = new ArrayList<>();//billCount more than 1
				billIDs.add("1234");
				billIDs.add("5657");
				
			
				List<String> billId = msg.getUrls();
				List<String> url= new ArrayList<String>();
				for(String id:billId) {
					url.add("http://" + domainName + "/v1/bill/"+id);

				}
				
				if (url.size()!= 0) {
					String HTMLBODY = "Your bill urls are below: ";
					for (String l:url) {
						HTMLBODY = HTMLBODY + "<a href=" + l + ">" + l + "</a>" + "<br/>";
					}
					sendEmail(FROM,TO,SUBJECT,TEXTBODY,HTMLBODY,context);
				}
			}
	
			timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
			context.getLogger().log("Invocation completed: " + timeStamp);
			return null;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	private static void sendEmail(String from, String to, String sub, String textBody, String htmlBody,Context context) {
		try {
		      AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
		      SendEmailRequest request = new SendEmailRequest().withDestination(new Destination().withToAddresses(to))
		    		  					.withMessage(new Message().withBody(new Body().withHtml(new Content()
		    		  							.withCharset("UTF-8").withData(htmlBody)).withText(new Content()
		    		  							.withCharset("UTF-8").withData(textBody))).withSubject(new Content()
		    		  							.withCharset("UTF-8").withData(sub))).withSource(from);
		          
		      client.sendEmail(request);
		      context.getLogger().log("Email sent!");
		      
		    } catch (Exception ex) {
		    	context.getLogger().log("The email was not sent. Error message: "+ex.getMessage()); 
		         
		    }
		}
	
	private long getExpiryTime(Context context) {
		try {
			TimeZone tz = TimeZone.getTimeZone("UTC");
			Calendar cal = Calendar.getInstance(tz);
			Date todayCal = cal.getTime();
			SimpleDateFormat crunchifyFor = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
			
			crunchifyFor.setTimeZone(tz);
			String curTime = crunchifyFor.format(todayCal);
			Date curDate;

			curDate = crunchifyFor.parse(curTime);
			Long epochTime = curDate.getTime() / 1000;
			long expiryTime = epochTime + (60 * 60);
			return expiryTime;
			
		} catch (ParseException e) {
			context.getLogger().log(e.getMessage());
		}
		return 0;

	}
} 
