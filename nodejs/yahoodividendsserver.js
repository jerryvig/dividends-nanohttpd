//Namespace declarations.
var mktneutral = mktneutral || {};
mktneutral.dividends = mktneutral.dividends || {};

/**
 * Constructor for the Yahoo dividends server.
 * 
 * @constructor
 *
 */
mktneutral.dividends.YahooDividendsServer = function() {
	 this.http = require('http');
	 this.fs = require('fs');
	 this.url = require('url');
	 this.zlib = require('zlib');
	 this.port = 80;
	 this.sqlite3 = require('sqlite3').verbose();
	 this.memoryDb = null;
	 this.diskDb = null;
	 this.pageData = new Object();
	 
	 this.loadMemoryDatabase();
	 this.loadFiles();
};

/**
 * Loads the static files into memory.
 * 
 */
mktneutral.dividends.YahooDividendsServer.prototype.loadFiles = function(){
	var self = this;
	
	this.loadStaticFile('./index.html', function(result){
		self.pageData.indexPage = result;
	});
	
	this.loadStaticFile('./js/yahoodividends.js', function(result){
		self.pageData.jsFile = result;
	});
	
   this.loadStaticFile('./css/yahoodividends.css', function(result){
	   self.pageData.cssFile = result;
   });
};

/**
 * Uses zlib to gzip a static file and then loads that file into a buffer for fast loading from the server.
 * 
 */
mktneutral.dividends.YahooDividendsServer.prototype.loadStaticFile = function(path,callback) {
	var self = this;
	this.fs.readFile(path, function(err, data){
		var buf = new Buffer(data, 'utf-8');
		self.zlib.gzip(buf, function(_, result){
			 callback(result);
		});
	});
};

/**
 *  Serves the http response data to the client.
 *  
 */
mktneutral.dividends.YahooDividendsServer.prototype.serve = function() {
	 var self = this;
     this.http.createServer(function(request, response){
    	 if ( request.url.indexOf('/getData') == 0 ) {
    	     var urlParts = self.url.parse(request.url, true);
    	     
    	     var offset = (urlParts.query.offset > 0) ?  urlParts.query.offset : 0;
    	     var sortColumn = (urlParts.query.sortColumn) ?  urlParts.query.sortColumn : 'yield';
    	     var sortOrder = (urlParts.query.sortOrder) ?  urlParts.query.sortOrder : 'desc';
    	     
    		 response.writeHead(200, {'Content-Type': 'application/json', 'Content-Encoding' : 'gzip'});
    	    
    	     self.doSQLQuery(offset, 30, sortColumn, sortOrder, function(jsonString){
    	    	 var buf = new Buffer(jsonString, 'utf-8');
    	 		 self.zlib.gzip(buf, function(_, result){
    	 			  response.end(result);
    	 		});
    	    });
    	 } else if ( request.url == '/' ) {
    		 response.writeHead(200, {'Content-Type': 'text/html', 'Content-Encoding' :  'gzip'});
     		 response.end( self.pageData.indexPage );
    	 } else if ( request.url == '/js/yahoodividends.js' ) {
    		 response.writeHead(200, {'Content-Type': 'application/javascript', 'Content-Encoding' :  'gzip'});
     		 response.end( self.pageData.jsFile );
    	 } else if ( request.url == '/css/yahoodividends.css' ) {
    		 response.writeHead(200, {'Content-Type': 'text/css', 'Content-Encoding' :  'gzip'});
     		 response.end( self.pageData.cssFile );
    	 }
     }).listen(this.port);	
     
     console.log( 'YahooDividendsServer started on port '+this.port + '. Waiting for connections.');
};


/**
 * Performs an SQL query against the sqlite database and returns the result in a JSON encoded string.
 * 
 * @param offset
 * @param limit
 * @param sortColumn
 * @param sortOrder
 */
mktneutral.dividends.YahooDividendsServer.prototype.doSQLQuery = function(offset, limit, sortColumn, sortOrder, callback){
	var self = this;
	var recordsArray = new Array();
	var responseString = '';
	
	var sqlString = 'SELECT * FROM full_data WHERE yield>0.0 AND yield!="null" ORDER BY ' + sortColumn + ' '  + sortOrder + ' LIMIT '+ limit + ' OFFSET '+ offset;
	console.log( sqlString );

	self.memoryDb.serialize(function(){
	 self.memoryDb.each(sqlString, function(err, row){
	     var record = new Object();
	     record.t = row.ticker;
	     record.n = (row.name == 'undefined' || row.name == null) ? '' : row.name;
	     record.mc = (row.market_cap == null) ? '' : row.market_cap;
	     record.l = row.last;
	     record.y = Math.round(row.yield*10000.0)/100.0+'%';
	     record.s = (row.sector == 'undefined' || row.sector == null) ? '' : row.sector;
	     record.i = (row.industry == 'undefined' || row.industry == null) ? '' : row.industry;
	     record.e = row.fte;
	     recordsArray.push( record );
	  }, function(){
		    var recordsObject = new Object();
			recordsObject.recs = recordsArray;
			responseString = JSON.stringify(recordsObject);
			callback(responseString);
	  });
	});
};

/**
 * Load the data from the disk file database into the memory database.
 * Creates the indexes on the memory database table full_data.
 * 
 */
mktneutral.dividends.YahooDividendsServer.prototype.loadMemoryDatabase = function() {
	var self = this;
	this.memoryDb = new this.sqlite3.Database(':memory:');
	
	this.memoryDb.serialize(function(){
	
	self.memoryDb.run('CREATE TABLE full_data ( ticker TEXT, name TEXT, market_cap TEXT, pe_ratio TEXT, eps TEXT, ttmd REAL, last REAL, yield REAL, index_membership TEXT, sector TEXT, industry TEXT, fte TEXT )');
	
     self.diskDb = new self.sqlite3.Database('/mnt/DriveTwo/dividends-nanohttpd/YahooDividends.db', self.sqlite3.OPEN_READONLY, function(){
    	self.diskDb.each('SELECT ticker, name, market_cap, pe_ratio, eps, ttmd, last, yield, index_membership, sector, industry, fte FROM full_data ORDER BY yield ASC', function(err, row){
    		self.memoryDb.run('INSERT INTO full_data (ticker, name, market_cap, pe_ratio, eps, ttmd, last, yield, index_membership, sector, industry, fte) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)',
    		 row.ticker, 
    		 row.name, 
    		 row.market_cap, 
    		 row.pe_ratio,
    		 row.eps,
    		 row.ttmd, 
    		 row.last, 
    		 row.yield, 
    		 row.index_membership, 
    	     row.sector,
    	     row.industry, 
    		 row.fte, function(err) {    		
    		 });
    		
    	}, function(){
        
		self.memoryDb.run('CREATE INDEX full_data_yield_index ON full_data(yield)', function(){
			self.memoryDb.run('CREATE INDEX full_data_ticker_index ON full_data(ticker)', function(){
				self.memoryDb.run('CREATE INDEX full_data_sector_index ON full_data(sector)', function(){
					self.memoryDb.run('CREATE INDEX full_data_industry_index ON full_data(industry)', function(){
						self.memoryDb.run('CREATE INDEX full_data_name_index ON full_data(name)', function(){
							self.memoryDb.run('CREATE INDEX full_data_ttmc_index ON full_data(ttmd)', function(){
								self.memoryDb.each('SELECT COUNT(*) FROM full_data', function(err, row){
							       console.log( row );
							    });
							});
						});
					});
				})
			});
		});
      });
     });
     
     
	});
};     

var yahooDividendsServer = new mktneutral.dividends.YahooDividendsServer();
yahooDividendsServer.serve();

