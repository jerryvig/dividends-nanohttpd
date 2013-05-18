/**
 * The documentation will go here.
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
	this.displayTable = document.getElementById('displayTable');
	this.nextButton = document.getElementById('nextButton');
	this.lastButton = document.getElementById('lastButton');
};

/**
 * Retrieves JSON data from the server and then adds that data to the display table of the page.
 *
 */
mktneutral.dividends.YahooDividends.prototype.getData = function(){
	var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function(){
		if ( xhr.readyState==4 ){
	    	if ( xhr.status==200 ){
	    		var i = 0;
	    		var recordsObject = JSON.parse(xhr.responseText);
	    		
	    		recordsObject.recs.forEach(function(record){
	    			var row = document.createElement('tr');
      	 			row.innerHTML = '<td><a href="http://finance.yahoo.com/q?s=' + record.t + '" target="_blank">' + record.t + '</td><td>' + record.n + '</td><td>' + record.s + '</td><td>' + record.i + '</td><td>' + record.mc + '</td><td>' + record.y + '</td>';
      	 			var ticker = document.createElement('tr');
      	 			 
	    			if ( i%2 == 0 ) {
      	 				row.setAttribute('class','white-row');
      	 			} else {
      	 				row.setAttribute('class','blue-row');
      	 			}
      	 			
      	 			this.displayTable.appendChild( row );
      				i++;
	    		});
	    	}
	    }
	}
	
	xhr.open('GET','/getData',true);
    xhr.send();	
};

/**
 * Click handler for the next button.
 *
 */
mktneutral.dividends.YahooDividends.prototype.nextButtonClickHandler = function(){
 	alert('you clicked the next button');
};

/**
 * Click handler for the last button.
 *
 */
mktneutral.dividends.YahooDividends.prototype.lastButtonClickHandler = function(){
 	alert('you clicked the last button');
};

window.onload = function(){
	var yahooDividends = new mktneutral.dividends.YahooDividends();
	yahooDividends.nextButton.onclick = yahooDividends.nextButtonClickHandler;
	yahooDividends.lastButton.onclick = yahooDividends.lastButtonClickHandler;
	
	yahooDividends.getData();
};



