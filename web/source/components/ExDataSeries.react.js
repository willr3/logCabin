
var React = require('react');
var CN = require('classnames');
var ExBar = require('./ExBar.react');

var ExDataSeries = React.createClass({
  getDefaultProps: function() {
    return {
      title: '',
      data: []
    }
  },

  render: function() {
    var props = this.props;
    var xScale = props.xScale;
    var yScale = props.yScale;

    var bars = _.map(this.props.data, function(point, i) {

      return (
        <ExBar height={yScale(point)} width={xScale.rangeBand()} offset={xScale(i)} availableHeight={props.height} color={props.color} key={i} />
      )
    });

    return (
      <g>{bars}</g>
    );
  }
});
module.exports = ExDataSeries;
