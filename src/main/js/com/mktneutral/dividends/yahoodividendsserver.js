importClass(java.lang.System);
importPackage(Packages.fi.iki.elonen);

var com = {};
com.mktneutral = {};
com.mktneutral.dividends = {};

/**
 * Constructor for the Yahoo dividends server.
 * 
 * @constructor
 *
 */
com.mktneutral.dividends.YahooDividendsServer = new JavaAdapter(NanoHTTPD, {	
	serve: function(uri, method, header, params, files){
		print(method + ' "' + uri + '" ');
		
		var response = new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "text/html", readFile('./index.html'));
		return response;
	},

	main: function(){
		try {
            this.start();
        } catch (e) {
            print("Couldn't start server:\n" + e);
            throw new Error('Aborting script');
        }

        print('Server started, Hit Enter to stop.\n');

        System.in.read();
        
        this.stop();
        print("Server stopped.\n");
	}
}, 8080);

com.mktneutral.dividends.YahooDividendsServer.main();



