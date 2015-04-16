function chart(options){
  var self = this;
  this.chart = document.getElementById(selection);
  this.clientWidth = this.chart.clientWidth;
  this.clientHeight = this.chart.clientHeight;


  var title,xlabel,ylabel;

  var xScale,yScale;
  var xAxis,yAxis;


  function rtrn(selection){

  }

  rtrn.render = function(){

  }
  rtrn.zoom = function(){
    
  }

  rtrn.title = function(value){
    if(!arguments.length) return title
    title = value;
    return rtrn;
  }
  rtrn.xLabel = function(value){
    if(!arguments.length) return xlabel;
    xlable = value;
    return rtrn;
  }
  rtrn.yLabel = function(value){
    if(!arguments.length) return ylable;
    ylable = value;
    return rtrn;
  }
  rtrn.xScale = function(value){
    if(!arguments.length) return xScale;
    xScale = value;
    return rtrn;
  }
  rtrn.yScale = function(value){
    if(!arguments.length) return yScale;
    yScale = value;
    return rtrn;
  }



  return rtrn;
}
