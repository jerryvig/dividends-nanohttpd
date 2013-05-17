package com.mktneutral.dividends;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

public class YahooDividendsUtils {
    /**
     * Loads the file from disk into memory as a GZIP byte[] image.
     * 
     * @param  path  The path to the file to load into memory.
     * @return  The byte array containing the GZIP image.
     */
    public static byte[] loadFileIntoMemory(String path) throws IOException {
    	BufferedReader pageReader = new BufferedReader(new FileReader( path ));
    	
    	String pageText = "";
    	String line;
    	while ( (line = pageReader.readLine()) != null ) {
    		pageText = pageText.concat(line.trim()+"\n");
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
    public static InputStream gzipCompressString(String str) throws IOException {
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
    public static InputStream bzip2CompressString(String str) throws IOException {
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
    
    /**
     * Serves the cached data for a page that is stored in GZIP format.
     * 
     * @return Response containing the InputStream with the index page data.
     */
    public static Response doPage(byte[] pageData, String contentType) {
        ByteArrayInputStream in = new ByteArrayInputStream(pageData);
     
        NanoHTTPD.Response response = new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, contentType, in);
        response.addHeader("Content-Encoding","gzip");
        return response;
    }
}
