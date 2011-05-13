package controllers;

import play.*;
import play.mvc.*;
import java.sql.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import models.*;
import play.db.jpa.JPA;
import play.mvc.results.RenderXml;
import play.mvc.results.Result;

public class Report extends Controller{
    
    public static void time()
    {
        Result playResult = null;
        
        String url = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        
        //Get the url from the app
        url = Play.configuration.getProperty("tinypm.db");
        
        try{
            Class.forName("com.mysql.jdbc.Driver").newInstance();

            connection = DriverManager.getConnection(url);
            statement = connection.createStatement();
            rs = statement.executeQuery("select tp.name, tp.description, tus.name, tus.description, tt.name, tt.description, tu.name, ta.timeSpent, ta.date from tpm_projects tp, tpm_user_stories tus, tpm_tasks tt, tpm_activities ta, tpm_users tu where tp.id = tus.projectId and tus.id = tt.userStoryId and tt.id = ta.taskId and ta.userId = tu.id");
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder  = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            
            Element root = document.createElement("report");
            document.appendChild(root);
            
            while(rs.next())
            {
                Element element = document.createElement("entry");
                
                element.setAttribute("projectName", rs.getString(1));
                element.setAttribute("projectDescription", rs.getString(2));
                element.setAttribute("storyName", rs.getString(3));
                element.setAttribute("storyDescription", rs.getString(4));
                element.setAttribute("taskName", rs.getString(5));
                element.setAttribute("taskDescription", rs.getString(6));
                element.setAttribute("userName", rs.getString(7));
                element.setAttribute("hours", rs.getString(8));
                element.setAttribute("date", rs.getString(9));
                
                root.appendChild(element);
            }
            
            //Close the result set
            rs.close();
            
            renderXml(document);
            
        }
        catch(Exception e)
        {
            System.out.println("Some bullshit happened");
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
        }
        
    }
}
