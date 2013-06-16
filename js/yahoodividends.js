/**
 * Copyright 2013 MktNeutral.com 
 * 
 * @fileoverview YahooDividends project for view dividend data for stocks and funds.
 * 
 * @author Jerry Vigil
 */

//Namespace declarations.
var mktneutral = mktneutral || {};
mktneutral.dividends = mktneutral.dividends || {};

/**
 * Create the new YahooDividends JavaScript object. 
 *
 * @constructor 
 */
mktneutral.dividends.YahooDividends = function(){
	this.offset = 0;
	this.sortOrder = 'desc';
	
	this.displayTable = document.getElementById('displayTable');
	this.nextButton = document.getElementById('nextButton');
	this.lastButton = document.getElementById('lastButton');
	this.yieldColumnHeader = document.getElementById('yieldColumnHeader');
	this.marketCapColumnHeader = document.getElementById('marketCapColumnHeader');
	this.tickerColumnHeader = document.getElementById('tickerColumnHeader');
	this.nameColumnHeader = document.getElementById('nameColumnHeader');
	this.sectorColumnHeader = document.getElementById('sectorColumnHeader');
	this.industryColumnHeader = document.getElementById('industryColumnHeader');
};

/**
 * Retrieves JSON data from the server and then adds that data to the display table of the page.
 *
 */
mktneutral.dividends.YahooDividends.prototype.getData = function(){
	var self = this;
	var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function(){
		if ( xhr.readyState==4 ){
	    	if ( xhr.status==200 ){
	    		var recordsObject = JSON.parse(xhr.responseText);
	    		self.updateDisplayTable(recordsObject.recs);
	    	}
	    }
	}
	
	xhr.open('GET','/getData',true);
    xhr.send();	
};

/**
 * Updates the data in the display table.
 * 
 * @param records  Object containing the records data to display in the displayTable.
 * 
 */
mktneutral.dividends.YahooDividends.prototype.updateDisplayTable = function(records){
	var self = this;
	var i = 0;
	
	this.removeTableRows();
		
	records.forEach(function(record){
		if ( record.n === undefined )
			record.n = '';
		
		if ( record.s === undefined ) 
			record.s = '';
		
		if ( record.i === undefined )
			record.i = '';
		
		if ( record.mc === undefined ) 
			record.mc = '';
		
		var row = document.createElement('tr');		
		row.innerHTML = '<td><a href="http://finance.yahoo.com/q/hp?s=' + record.t + '&g=v" target="_blank">' + record.t + '</td><td>' + record.n + '</td><td>' + record.s + '</td><td>' + record.i + '</td><td>' + record.mc + '</td><td class="yieldCell">' + record.y + '</td>';
			 
		if ( i%2 == 0 ) {
			row.setAttribute('class','white-row');
		} else {
			row.setAttribute('class','blue-row');
		}

		self.displayTable.appendChild( row );
		i++;
	});
};

/**
 * Removes all of the data rows from the display table.
 */
mktneutral.dividends.YahooDividends.prototype.removeTableRows = function(){
	var tableRows = this.displayTable.getElementsByTagName('tr');
	var self = this;
	
	while ( tableRows.length > 2 ) {
		tableRows = this.displayTable.getElementsByTagName('tr');
		this.displayTable.removeChild( tableRows.item(tableRows.length-1) );	
	}
};

/**
 * Click handler for the next button.
 * 
 *	The offset is incremented up by 30 each time the next button is clicked.
 */
mktneutral.dividends.YahooDividends.prototype.nextButtonClickHandler = function(){
	var self = this;
	var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function(){
		if ( xhr.readyState==4 ){
	    	if ( xhr.status==200 ){
	    		var recordsObject = JSON.parse(xhr.responseText);
	    		self.updateDisplayTable(recordsObject.recs);
	    	}
	    }
	}
	
    self.offset += 30;
	xhr.open('GET','/getData?offset='+self.offset,true);
    xhr.send();	
};

/**
 * Click handler for the last button.
 *
 */
mktneutral.dividends.YahooDividends.prototype.lastButtonClickHandler = function(){
	var self = this;
	var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function(){
		if ( xhr.readyState==4 ){
	    	if ( xhr.status==200 ){
	    		var recordsObject = JSON.parse(xhr.responseText);
	    		self.updateDisplayTable(recordsObject.recs);
	    	}
	    }
	}
	
    if ( self.offset > 0 ) {
    	self.offset -= 30;
    }
    
    xhr.open('GET','/getData?offset='+self.offset,true);
    xhr.send();	
};

/**
 * Click handler for the yield header.
 *
 */
mktneutral.dividends.YahooDividends.prototype.yieldHeaderClickHandler = function(){
	var self = this;
	var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function(){
		if ( xhr.readyState==4 ){
	    	if ( xhr.status==200 ){
	    		var recordsObject = JSON.parse(xhr.responseText);
	    		self.updateDisplayTable(recordsObject.recs);
	    	}
	    }
	}
	
    self.offset = 0;
    self.sortOrder = (self.sortOrder == 'desc') ? 'asc' : 'desc';
    
    xhr.open('GET','/getData?offset='+self.offset+'&sortColumn=yield&sortOrder='+self.sortOrder,true);
    xhr.send();	
};

//Main execution begins here.
var yahooDividends = new mktneutral.dividends.YahooDividends();
yahooDividends.nextButton.onclick = function(){
	yahooDividends.nextButtonClickHandler();
};
	
yahooDividends.lastButton.onclick = function(){
	yahooDividends.lastButtonClickHandler();
};

yahooDividends.yieldColumnHeader.onclick = function(){
	yahooDividends.yieldHeaderClickHandler();
};
	
yahooDividends.getData();