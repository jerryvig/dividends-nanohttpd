/**
 * Copyright 2013 MktNeutral.com 
 * 
 * @fileoverview YahooDividends project for view dividend data for stocks and funds.
 *
 */

//Namespace declarations.
var mktneutral = {};
mktneutral.dividends = {};

/**
 * Create the new YahooDividends JavaScript object. 
 *
 * @constructor 
 */
mktneutral.dividends.YahooDividends = function(){
	this.offset = 0;
	
	this.displayTable = document.getElementById('displayTable');
	this.nextButton = document.getElementById('nextButton');
	this.lastButton = document.getElementById('lastButton');
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
		var row = document.createElement('tr');
		row.innerHTML = '<td><a href="http://finance.yahoo.com/q?s=' + record.t + '" target="_blank">' + record.t + '</td><td>' + record.n + '</td><td>' + record.s + '</td><td>' + record.i + '</td><td>' + record.mc + '</td><td>' + record.y + '</td>';
			 
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

window.onload = function(){
	var yahooDividends = new mktneutral.dividends.YahooDividends();
	yahooDividends.nextButton.onclick = function(){
		yahooDividends.nextButtonClickHandler();
	};
	
	yahooDividends.lastButton.onclick = function(){
		yahooDividends.lastButtonClickHandler();
	};
	
	yahooDividends.getData();
};



