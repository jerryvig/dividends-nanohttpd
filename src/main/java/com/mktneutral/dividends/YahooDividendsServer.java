package com.mktneutral.dividends;

import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.ServerRunner;

/**
 * An example of subclassing NanoHTTPD to make a custom HTTP server.
 */
public class YahooDividendsServer extends NanoHTTPD {
    private Logger logger = LoggerFactory.getLogger(YahooDividendsServer.class);
    private Connection memoryConnection;
    private String responseString;
    private DecimalFormat pctFormat = new DecimalFormat("#,###.##%");
    private DecimalFormat dollarFormat = new DecimalFormat("#,###.00");
    
    private byte[] indexPageData;
    private byte[] jsFileData;
    private byte[] cssFileData;
    
    public YahooDividendsServer() {
        super(8080);

        try {
        	loadMemoryDatabase();
        	indexPageData = loadFileIntoMemory("./index.html");
        	jsFileData = loadFileIntoMemory("js/yahoodividends.js");
        	cssFileData = loadFileIntoMemory("css/yahoodividends.css");
        } catch ( ClassNotFoundException cnfe ) {
        	cnfe.printStackTrace();
        } catch ( IOException ioe ) {
        	ioe.printStackTrace();
        }
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parms, Map<String, String> files) {
        logger.info(method + " '" + uri + "' ");
        
        if ( uri.equals("/") ) {
            return doPage(indexPageData, "text/html");
        }
        else if ( uri.equals("/js/yahoodividends.js") ) {
        	return doPage(jsFileData, "application/javascript");
        }
        else if ( uri.equals("/getData") ) {
        	return doGetDataPage();
        }
        else if ( uri.equals("/css/yahoodividends.css") ) {
        	return doPage(cssFileData, "text/css");
        }
        else {
        	return doPage(indexPageData, "text/html");
        }
    }
    
    /**
     * Serves the cached data for the index page.
     * 
     * @return Response containing the InputStream with the index page data.
     */
    public Response doPage(byte[] pageData, String contentType) {
        ByteArrayInputStream in = new ByteArrayInputStream(pageData);
     
        NanoHTTPD.Response response = new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, contentType, in);
        response.addHeader("Content-Encoding","gzip");
        return response;
    }
    
    /**
     * 
     * @return
     */
    public Response doGetDataPage() {
        buildResponse();
        
        InputStream in = null;
        try {
        	in = gzipCompressString(responseString);
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

    public void buildResponse() {
    	JSONObject recordsObject = new JSONObject();
    	JSONArray recordsArray = new JSONArray();
    	
        try {
        	Statement stmt = memoryConnection.createStatement();
        
        	ResultSet resultSet = stmt.executeQuery("SELECT * FROM full_data WHERE (yield>0.08 AND yield<0.20) ORDER BY yield DESC LIMIT 100");
        	while ( resultSet.next() ) {
        		JSONObject record = new JSONObject();
        		try {
        			record.put("t", resultSet.getString(1));
        			record.put("n", resultSet.getString(2));
        			record.put("mc", resultSet.getString(3));
        			record.put("pe", resultSet.getString(4));
        			record.put("ep", resultSet.getString(5));
        			record.put("tt", dollarFormat.format(resultSet.getDouble(6)));
        			record.put("l", dollarFormat.format(resultSet.getDouble(7)));
        			record.put("y", pctFormat.format(resultSet.getDouble(8)));
        			record.put("im", resultSet.getString(9));
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
        		
        	responseString = recordsObject.toString();
        	responseString = responseString.replace("undefined", "undef");
        } catch ( SQLException sqle ) {
        	sqle.printStackTrace();
        }
    }
    
    
    /**
     *  Loads data from the database on disk (YahooDividends.db) into the sqlite in memory database.
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
    
    /**
     * Loads the file from disk into memory as a GZIP byte[] image.
     * 
     * @return  The byte array containing the GZIP image.
     */
    public byte[] loadFileIntoMemory(String path) throws IOException {
    	BufferedReader pageReader = new BufferedReader(new FileReader( path ));
    	
    	String pageText = "";
    	String line;
    	while ( (line = pageReader.readLine()) != null ) {
    		pageText = pageText.concat(line.trim());
    	}
    	
    	pageReader.close();
    	
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	GZIPOutputStream gzip = new GZIPOutputStream(out, Deflater.BEST_COMPRESSION);
    	OutputStreamWriter writer = new OutputStreamWriter(gzip);
    	writer.write(pageText);
    	writer.close();
    	out.close();
    	
    	return out.toByteArray();
    } 
    
    /**
     * Takes a string and compresses it in GZIP format. The output is a InputStream to be 
     * written on the HTTP response body.
     * 
     * @param str  Input string to compress.
     * @return InputStream the gzip compress stream that will written on the HTTP response body.
     * @throws IOException
     */
    public InputStream gzipCompressString(String str) throws IOException {
    	ByteArrayInputStream in = null;
    	
    	if ( str == null || str.length() == 0 ) {
    		return in;
    	}
    	
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	GZIPOutputStream gzip = new GZIPOutputStream(out, Deflater.BEST_COMPRESSION);
    	OutputStreamWriter writer = new OutputStreamWriter(gzip);
    	writer.write(str);
    	writer.close();
    	out.close();
    	
    	in = new ByteArrayInputStream(out.toByteArray());
    	return in;
    }
    
    /**
     * Takes a string and compresses it in BZIP2 format. The output is a InputStream to be 
     * written on the HTTP response body.
     * 
     * @param str  Input string to compress.
     * @return InputStream the gzip compress stream that will written on the HTTP response body.
     * @throws IOException
     */
    public InputStream bzip2CompressString(String str) throws IOException {
    	ByteArrayInputStream in = null;
    	
    	if ( str == null || str.length() == 0 ) {
    		return in;
    	}
    	
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	BZip2CompressorOutputStream gzip = new BZip2CompressorOutputStream(out);
    	OutputStreamWriter writer = new OutputStreamWriter(gzip);
    	writer.write(str);
    	writer.close();
    	out.close();
    	
    	in = new ByteArrayInputStream(out.toByteArray());
    	return in;
    }
}
