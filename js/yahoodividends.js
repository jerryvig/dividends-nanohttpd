/**
 * The documentation will go here.
 *
 */

$(document).ready(function(){
   $.getJSON('/getData',function(data){
      var records = data.recs;
      var i = 0;     
      $.each(records,function(idx,record){
         if ( i%2 == 0 ) {
      	 	var row = '<tr class="white-row"><td>' + record.t + '</td><td>' + record.n + '</td><td>' + record.s + '</td><td>' + record.i + '</td><td>' + record.mc + '</td><td>' + record.y + '</td></tr>';
      	 } else {
      	 	var row = '<tr class="blue-row"><td>' + record.t + '</td><td>' + record.n + '</td><td>' + record.s + '</td><td>' + record.i + '</td><td>' +  record.mc + '</td><td>' + record.y + '</td></tr>';
      	 }
      	 $('#displayTable').append( row );
      	 i++;
      });
   });   
});



