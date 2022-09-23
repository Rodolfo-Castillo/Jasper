/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package jasper;
import org.json.*;
import static com.bea.xml.stream.SubReader.print;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.design.JRDesignQuery;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.view.JasperViewer;
/**
 *
 * @author caiet
 */
public class Jasper {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try{
            HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);
            
            server.createContext("/info", new InfoHandler());
            
            server.createContext("/get", new GetHandler());

            server.setExecutor(null); // creates a default executor

            server.start();

            System.out.println("The server is running");
        }catch(IOException e){
            
        }
    }
    
    static class InfoHandler implements HttpHandler {

    public void handle(HttpExchange httpExchange) throws IOException {

      String response = "Use /get?hello=word&foo=bar to see how to handle url parameters";

      Jasper.writeResponse(httpExchange, response.toString());

    }

  }
    
    static class GetHandler implements HttpHandler {

        public void handle(HttpExchange httpExchange) throws IOException {

            StringBuilder response = new StringBuilder();

            Map<String, Object> params = Jasper.queryToMap(httpExchange.getRequestURI().getQuery());
            
            String b64 = getTicketBase64(params);
            if(b64 != null){
                response.append(b64);
            }else{
                response.append("Algo salio mal");
            }
            
            Jasper.writeResponse(httpExchange, response.toString());

        }

    }
    
    public static void writeResponse(HttpExchange httpExchange, String response)
            throws IOException {
        httpExchange.sendResponseHeaders(200, response.length());

        OutputStream os = httpExchange.getResponseBody();

        os.write(response.getBytes());

        os.close();
    }
    
    public static Map<String, Object> queryToMap(String query) {

        Map<String, Object> result = new HashMap<String, Object>();

        byte[] decodedURLBytes = Base64.getUrlDecoder().decode(query);

        String actualURL = new String(decodedURLBytes);

        for (String param : actualURL.split("&")) {

            String pair[] = param.split("=");

            if (pair.length > 1) {

                result.put(pair[0], pair[1]);

            } else {

                result.put(pair[0], "");

            }

        }
        
        return result;

    }
    
    public static String getTicketBase64(Map<String, Object> params){
        String pdfBase64 = "";
        Connection conn = null;
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            try{
                conn = DriverManager.getConnection("jdbc:mysql://localhost/Tienda","root","");
            }
            catch(SQLException e){
                System.err.println(e.getMessage());
            }
            String query = "";
            try{
            JasperDesign jdesign = JRXmlLoader.load(System.getProperty("user.dir")+"/src/jasper/reportes/"+params.get("reporte")+".jrxml");
                switch ((String)params.get("reporte")){
                    case "ticket":
                        query = "CALL GetTicket("+params.get("p_idVenta")+",'";
                    break;
                    case "ReporteVentas":
                        query = "CALL GetVentasDeCorte('"+params.get("p_idVentas")+"','"+params.get("p_desde")+"','"+params.get("p_hasta")+"','";
                    break;
                }
                query += params.get("token")+"');";
                JRDesignQuery updateQuery = new JRDesignQuery();
                updateQuery.setText(query);
                jdesign.setQuery(updateQuery);
                JasperReport jreport = JasperCompileManager.compileReport(jdesign);
                JasperPrint jprint = JasperFillManager.fillReport(jreport, null,conn);
                //JasperViewer.viewReport(jprint);
                byte[] pdf = JasperExportManager.exportReportToPdf(jprint);
                pdfBase64 = "data:application/pdf;base64,"+Base64.getEncoder().encodeToString(pdf);
                return pdfBase64;
            }catch(JRException e){
                System.err.println(e.getMessage());
                return null;
            }
        }catch(ClassNotFoundException e){
            System.err.println(e.getMessage());
            return null;
        }
    }
}