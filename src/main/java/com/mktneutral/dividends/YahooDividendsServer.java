package com.mktneutral.dividends;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.ServerRunner;

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class YahooDividendsServer extends NanoHTTPD {
    private Logger logger = LoggerFactory.getLogger(YahooDividendsServer.class);
    private Connection memoryConnection;
    private DecimalFormat pctFormat = new DecimalFormat("#,###.#%");
    private DecimalFormat dollarFormat = new DecimalFormat("#,###.00");
    
    private HashMap<String,byte[]> pageData = new HashMap<String,byte[]>();
    
    /**
     * Initialize the application here. Load data into memory from here.
     * 
     * @constructor
     */
    public YahooDividendsServer() {
        super(80);

        try {
        	loadMemoryDatabase();
        	
        	pageData.put("indexPage", YahooDividendsUtils.loadFileIntoMemory("./index.html"));
        	pageData.put("jsFile", YahooDividendsUtils.loadFileIntoMemory("js/yahoodividends.js"));
        	pageData.put("cssFile", YahooDividendsUtils.loadFileIntoMemory("css/yahoodividends.css"));
        } catch ( ClassNotFoundException cnfe ) {
        	cnfe.printStackTrace();
        } catch ( IOException ioe ) {
        	ioe.printStackTrace();
        }
    }

    /**
     * The main serve method that is overridden from the nanohttpd framework.
     * 
     */
    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
        logger.info(method + " '" + uri + "' ");
        
        if ( uri.equals("/") ) {
            return YahooDividendsUtils.doPage(pageData.get("indexPage"), MediaType.TEXT_HTML);
        }
        else if ( uri.equals("/js/yahoodividends.js") ) {
        	return YahooDividendsUtils.doPage(pageData.get("jsFile"),  "application/javascript");
        }
        else if ( uri.equals("/getData") ) {
        	return doGetDataPage(parms);
        }
        else if ( uri.equals("/css/yahoodividends.css") ) {
        	return YahooDividendsUtils.doPage(pageData.get("cssFile"), "text/css");
        }
        else {
        	return YahooDividendsUtils.doPage(pageData.get("indexPage"), MediaType.TEXT_HTML);
        }
    }
    
    
    /**
     * Returns Response and processes the query 
     * 
     * @param  params The Map containing the HTTP GET request parameters.
     * @return Response  The NanoHTTPD response that will be sent to the client.
     */
    public Response doGetDataPage(Map<String, String> params) {
    	int offset = 0;
    	if ( params.get("offset") != null ) {
    		try {
    			offset = Integer.parseInt(params.get("offset"));
    		} catch ( NumberFormatException nfe ) {
    			nfe.printStackTrace();
    		}
    	}
    	
    	int limit = 30;
    	if ( params.get("limit") != null ) {
    		try {
    			limit = Integer.parseInt(params.get("limit"));
    		} catch ( NumberFormatException nfe ) {
    			nfe.printStackTrace();
    		}
    	}
    	
    	String sortColumn = "yield";
    	String sortOrder = "desc";
    	
    	if ( params.get("sortColumn") != null ) {
            sortColumn = params.get("sortColumn").trim();
    	}
    	
    	if ( params.get("sortOrder") != null ) {
    		sortOrder = params.get("sortOrder").trim();
    	}
    	
    	String responseString = doSQLQuery(offset, limit, sortColumn, sortOrder);
        
        InputStream in = null;
        try {
        	in = YahooDividendsUtils.gzipCompressString(responseString);
        } catch ( IOException ioe ) {
        	ioe.printStackTrace();
        }

        NanoHTTPD.Response response = new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "application/json", in);
        response.addHeader("Content-Encoding","gzip");
        return response;
    }

    /**
     *  Main method to run the server.
     */
    public static void main(String[] args) {
        ServerRunner.run(YahooDividendsServer.class);
    }

    /**
     * Performs and SQL query against the sqlite database and returns the result in a JSON encoded string.
     * 
     * @param offset
     * @param limit
     * @return String  A JSON representation of the SQL query result.
     */
    public String doSQLQuery(int offset, int limit, String sortColumn, String sortOrder) {
    	JSONObject recordsObject = new JSONObject();
    	JSONArray recordsArray = new JSONArray();
    	
        try {
        	Statement stmt = memoryConnection.createStatement();
        
        	ResultSet resultSet = stmt.executeQuery("SELECT * FROM full_data WHERE yield>0.0 ORDER BY " + sortColumn + " "  + sortOrder + " LIMIT "+ Integer.toString(limit) + " OFFSET "+ Integer.toString(offset));
        	while ( resultSet.next() ) {
        		JSONObject record = new JSONObject();
        		try {
        			record.put("t", resultSet.getString(1));
        			record.put("n", resultSet.getString(2));
        			record.put("mc", resultSet.getString(3));
        			// record.put("pe", resultSet.getString(4));
        			// record.put("ep", resultSet.getString(5));
        			// record.put("tt", dollarFormat.format(resultSet.getDouble(6)));
        			record.put("l", dollarFormat.format(resultSet.getDouble(7)));
        			record.put("y", pctFormat.format(resultSet.getDouble(8)));
        			// record.put("im", resultSet.getString(9));
        			record.put("s", resultSet.getString(10));
        			record.put("i", resultSet.getString(11));
        			record.put("ft", resultSet.getString(12));
        			
        		} catch( JSONException jsone ) {
        			jsone.printStackTrace();
        		}
        		
        		recordsArray.put( record );
        	}
        	
        	resultSet.close();
        	try {
        		recordsObject.put("recs", recordsArray);
        	} catch ( JSONException jsone ) {
        		jsone.printStackTrace();
        	}
        		
        	String responseString = recordsObject.toString();
        	responseString = responseString.replace("undefined", "");
        	return responseString;
        	
        } catch ( SQLException sqle ) {
        	sqle.printStackTrace();
        	return "";
        }
    }
    
    
    /**
     *  Loads data from the sqlite database on disk (YahooDividends.db) into the sqlite in memory database.
     *  
     */
    public void loadMemoryDatabase() throws ClassNotFoundException {
    	Class.forName("org.sqlite.JDBC");

    	Connection conn = null;

    	try {
    		  memoryConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
    	} catch ( SQLException sqle ) {
    		  sqle.printStackTrace();
    	}
    	
    	if ( memoryConnection != null ) {
    		try {
    			conn = DriverManager.getConnection("jdbc:sqlite:YahooDividends.db");
        
    			PreparedStatement memoryStmt = memoryConnection.prepareStatement("CREATE TABLE full_data ( ticker TEXT, name TEXT, market_cap TEXT," +
    					" pe_ratio TEXT, eps TEXT, ttmd REAL, last REAL, yield REAL, index_membership TEXT, sector TEXT, industry TEXT, fte TEXT )");
    			memoryStmt.executeUpdate();
    			
    			memoryStmt = memoryConnection.prepareStatement("INSERT INTO full_data (ticker, name, market_cap, pe_ratio, eps, ttmd, last, yield, index_membership, sector, industry, fte) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
    			
    			Statement stmt = conn.createStatement();
    			ResultSet resultSet = stmt.executeQuery("SELECT ticker, name, market_cap, pe_ratio, eps, ttmd, last, yield, index_membership, sector, industry, fte FROM full_data ORDER BY yield DESC");
    			
    			while ( resultSet.next() ) {
    				memoryStmt.setString(1, resultSet.getString(1));
    				memoryStmt.setString(2, resultSet.getString(2));
    				memoryStmt.setString(3, resultSet.getString(3));
    				memoryStmt.setString(4, resultSet.getString(4));
    				memoryStmt.setString(5, resultSet.getString(5));
    				memoryStmt.setDouble(6, resultSet.getDouble(6));
    				memoryStmt.setDouble(7, resultSet.getDouble(7));
    				memoryStmt.setDouble(8, resultSet.getDouble(8));
    				memoryStmt.setString(9, resultSet.getString(9));
    				memoryStmt.setString(10, resultSet.getString(10));
    				memoryStmt.setString(11, resultSet.getString(11));
    				memoryStmt.setString(12, resultSet.getString(12));
    				
    				memoryStmt.executeUpdate();
    			}
    			
    			resultSet.close();
    			
    			//Create the indexes on the full_data table in the memory database.
    			memoryStmt = memoryConnection.prepareStatement("CREATE INDEX full_data_yield_index ON full_data(yield)");
    			memoryStmt.executeUpdate();
    			memoryStmt = memoryConnection.prepareStatement("CREATE INDEX full_data_ticker_index ON full_data(ticker)");
    			memoryStmt.executeUpdate();
    			memoryStmt = memoryConnection.prepareStatement("CREATE INDEX full_data_sector_index ON full_data(sector)");
    			memoryStmt.executeUpdate();
    			memoryStmt = memoryConnection.prepareStatement("CREATE INDEX full_data_industry_index ON full_data(industry)");
    			memoryStmt.executeUpdate();
    			memoryStmt = memoryConnection.prepareStatement("CREATE INDEX full_data_name_index ON full_data(name)");
    			memoryStmt.executeUpdate();
    			memoryStmt = memoryConnection.prepareStatement("CREATE INDEX full_data_ttmc_index ON full_data(ttmd)");
    			memoryStmt.executeUpdate();
    			
    		} catch ( SQLException sqle ) {
    			sqle.printStackTrace();
    		} finally {
    			try {
    				if ( conn != null ) {
    					conn.close();
    				}
    			} catch ( SQLException sqle ) {
    				sqle.printStackTrace();
    			}
    		} 
    	}
    }
}
