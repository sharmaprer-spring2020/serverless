

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

	public Object handleRequest(SNSEvent request, Context context) {
		try {
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
			context.getLogger().log("Invocation started: " + timeStamp);
			context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());
			ObjectMapper objMap = new ObjectMapper();
			// convert json message to object
			NotificationMessage msg = objMap.readValue(request.getRecords().get(0).getSNS().getMessage(),
					NotificationMessage.class);
	
			// check if the dynamoDB has the item with userId
			Table table = dynamoDB.getTable(tableName);
			// table.getitem
			Item fetchItem = table.getItem("dbId", msg.getUserId());
			if (fetchItem == null) {
				Item item = new Item().withPrimaryKey("dbId", 120).with("TTL", getExpiryTime());
				table.putItem(item);
				sendEmail();
			}
	
			timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
			context.getLogger().log("Invocation completed: " + timeStamp);
			return null;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	private static void sendEmail() {
		String FROM = "sender@dev.prernasharma.me";

		  // Replace recipient@example.com with a "To" address. If your account
		  // is still in the sandbox, this address must be verified.
		  String TO = "sharma.prer@husky.neu.edu";

		  // The configuration set to use for this email. If you do not want to use a
		  // configuration set, comment the following variable and the 
		  // .withConfigurationSetName(CONFIGSET); argument below.
		  //String CONFIGSET = "ConfigSet";

		  // The subject line for the email.
		  String SUBJECT = "Amazon SES test (AWS SDK for Java)";
		  
		  // The HTML body for the email.
		  String HTMLBODY = "<h1>Amazon SES test (AWS SDK for Java)</h1>"
		      + "<p>This email was sent with <a href='https://aws.amazon.com/ses/'>"
		      + "Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>" 
		      + "AWS SDK for Java</a>";

		  // The email body for recipients with non-HTML email clients.
		  String TEXTBODY = "This email was sent through Amazon SES "
		      + "using the AWS SDK for Java.";


		    try {
		      AmazonSimpleEmailService client = 
		          AmazonSimpleEmailServiceClientBuilder.standard()
		          // Replace US_WEST_2 with the AWS Region you're using for
		          // Amazon SES.
		            .withRegion(Regions.US_EAST_1).build();
		      SendEmailRequest request = new SendEmailRequest()
		          .withDestination(
		              new Destination().withToAddresses(TO))
		          .withMessage(new Message()
		              .withBody(new Body()
		                  .withHtml(new Content()
		                      .withCharset("UTF-8").withData(HTMLBODY))
		                  .withText(new Content()
		                      .withCharset("UTF-8").withData(TEXTBODY)))
		              .withSubject(new Content()
		                  .withCharset("UTF-8").withData(SUBJECT)))
		          .withSource(FROM);
		          // Comment or remove the next line if you are not using a
		          // configuration set
		          //.withConfigurationSetName(CONFIGSET);
		      client.sendEmail(request);
		      System.out.println("Email sent!");
		    } catch (Exception ex) {
		      System.out.println("The email was not sent. Error message: " 
		          + ex.getMessage());
		    }
		}
	private static long getExpiryTime() {
		try {
			TimeZone timeZone = TimeZone.getTimeZone("UTC");
			Calendar cali = Calendar.getInstance(timeZone);
			Date todayCal = cali.getTime();
			SimpleDateFormat crunchifyFor = new SimpleDateFormat("MMM dd yyyy HH:mm:ss.SSS zzz");
			// SimpleDateFormat crunchifyFormat = new SimpleDateFormat("MMM dd yyyy
			// HH:mm:ss.SSS zzz");
			crunchifyFor.setTimeZone(timeZone);
			String curTime = crunchifyFor.format(todayCal);
			Date curDate;

			curDate = crunchifyFor.parse(curTime);
			Long epoch = curDate.getTime() / 1000;
			long expiryTime = epoch + (60 * 60);
			return expiryTime;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;

	}
} 
