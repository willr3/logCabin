
var React = require('react');
var CN = require('classnames');

var d3 = require('d3');

var ExChart = require('./ExChart.react');
var ExDataSeries = require('./ExDataSeries.react');

var App = React.createClass({
  getDefaultProps : function(){
    return {
      width: 600,
      height: 400
    }
  },
  render : function(){
    var data = [ 30, 10, 5, 8, 15, 10 ];
    var yScale = d3.scale.linear()

    var foo = (d) => d;
    var yScale = d3.linear.scale()
    .domain([0, d3.max(data)])
    .range([0, this.props.height]);

    var xScale = d3.scale.ordinal()
    .domain(d3.range(data.length))
    .rangeRoundBands([0, this.props.width], 0.05);

    return (
      <ExChart width={this.props.width} height={this.props.height}>
      <ExDataSeries xScale={xScale} yScale={yScale} data={data} width={this.props.width} height={this.props.height} color="cornflowerblue" />
      </ExChart>
    )
  }
})

module.exports = App;
