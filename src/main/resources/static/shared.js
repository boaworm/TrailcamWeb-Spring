Date.prototype.getWeek = function() {
  var onejan = new Date(this.getFullYear(),0,1);
  var today = new Date(this.getFullYear(),this.getMonth(),this.getDate());
  var dayOfYear = ((today - onejan + 86400000)/86400000);
  return Math.ceil(dayOfYear/7)
};

function formatDate(date){
	try {
		const y = date.getFullYear();
		const m = (date.getMonth()+1).toString().padStart(2,'0');
		const d = date.getDate().toString().padStart(2,'0');
		return y + "-" + m + "-" + d;
	} catch (error){
		console.log("ERROR: invoking dateFormat(...) with NULL");
		console.log("date object = ", date);
		console.log("ERROR = ", error);
	}
	return "2000-01-01";
}

function isDateInRange(y,m,d){
	const str = y + "-" + m.padStart(2,'0') + "-" + d.padStart(2,'0');
 	const tmpDate = new Date(str);
    var ret = (tmpDate >= dateRangeFrom && tmpDate <= dateRangeTo);
	return ret;
 }

function transformMapToD3DataArray(theMap) {
    m = [];
    for( const [key, value] of theMap.entries()){
		const o = {};
		o["year"] = key;
		o["observations"] = value;
		m.push(o);
    }
    return m;
}
const cameraArray = ["TOP","MIDDLE","HOUSE"];
const monthArray = ["January","February","March","April","May","June","July","August","September","October","November","December"];
function numericToTextMonth(month){
	return monthArray[ month -1 ];
}

function calculateMinMaxDateRange(DATA){
	var oD = new Date();
	var nD = new Date();
    // First and last day in the dataset
    DATA.forEach((image) => {
        const tmpDate = new Date(image.year, (image.month-1), image.day);
        if(tmpDate < oD){
			oD = new Date(tmpDate);
        }
        if(tmpDate > nD){
			nD = new Date(tmpDate);
        }
    });
	return [oD, nD];
}



// Repeatable graph creation, StackedHBAR
function createStackedHBarGraph(divName, title, dataObject, barLabel, groupLabel, valueLabel,  widthScale = 1, heightScale = 1){

	// Remove old SVGs
	const myNode = document.getElementById(divName.substring(1));
	while(myNode && myNode.firstChild){
		myNode.removeChild(myNode.lastChild);
	}

    let margin = {top: 30, right: 30, bottom: 70, left: 60},
	width = (460*widthScale) - margin.left - margin.right,
	height = (400*heightScale) - margin.top - margin.bottom;
	// expect obj { barLabel: ... , groupLabel: ... , valueLabel: ... } 
	// for ex	{ year: 2010, camera: "TOP", observations: 100 }

	// figure out X labels, and combined max X label (for Y scale)
	var distinctBarLabels = [];
	dataObject.forEach( (row) => {
		if( distinctBarLabels.indexOf(row[barLabel]) === -1 ){
			distinctBarLabels.push( row[barLabel] );
		}
	});
	//console.log("distinct bar labels: ", distinctBarLabels);
	var distinctGroupLabels = [];
	dataObject.forEach( (row) =>{
		if(distinctGroupLabels.indexOf(row[groupLabel]) === -1){
			distinctGroupLabels.push(row[groupLabel]);
		}
	});
	//console.log("distinct group labels: ", distinctGroupLabels);

	let maxPerRow = 0;
	distinctBarLabels.forEach( (row) => {
		const subset = dataObject.filter( function(d) {
			return d[barLabel] === row;
		});
		let sum = subset.reduce( function(a,d){
			return a + +(d[valueLabel]);
		},0);
		if(sum > maxPerRow){
			maxPerRow = sum;
		}
	});

	//console.log("Max accumulated value for one row is: " , maxPerRow);

     // append the svg object to the body of the page
    let svg = d3.select(divName)
		.append("svg")
		.attr("width", width + margin.left + margin.right)
		.attr("height", height + margin.top + margin.bottom)
		.append("g")
			.attr("transform",
				"translate(" + margin.left + "," + margin.top + ")");

    // Specific to Stacked BHar
	var color = d3.scaleOrdinal()
		.domain(distinctBarLabels)
		.range(d3.schemeSet2);

	// Add X axis
	var x = d3.scaleBand()
		.domain(distinctBarLabels)
		.range([0, width])
		.padding([0.2])

    svg.append("g")
		.attr("transform", "translate(0," + height + ")")
		.call(d3.axisBottom(x))
			.selectAll("text")
			.attr("transform", "translate(-10,0)rotate(-45)")
			.style("text-anchor", "end");

	// Add Y axis
	var y = d3.scaleLinear()
		.domain([0, Math.ceil( 1.1 * maxPerRow)])
		.range([ height, 0 ]);
	svg.append("g")
		.call(d3.axisLeft(y));

	var stackedData = d3.stack()
		.keys(d3.union(dataObject.map(d => d[groupLabel])))
		.value(([,group], key) => group.get(key).observations)
		(d3.index(dataObject, d => d[barLabel], d => d[groupLabel]));

	// Show the bars
	svg.append("g")
		.selectAll("g")
		// Enter in the stack data = loop key per key = group per group
		.data(stackedData)
		.enter().append("g")
			.attr("fill", function(d) { return color(d.key); })
			.selectAll("rect")
			// enter a second time = loop subgroup per subgroup to add all rectangles
			.data(function(d) { return d; })
				.enter().append("rect")
					.attr("x", function(d) { return x(d.data[0]); })
					.attr("y", function(d) { return y(d[1]); })
					.attr("height", function(d) { return y(d[0]) - y(d[1]); })
					.attr("width",x.bandwidth());

	var keys = distinctGroupLabels;
	svg.selectAll("mydots")
		.data(keys)
		.enter()
		.append("circle")
			.attr("cx", function(d,i){ return 30 + i*100;})
			.attr("cy", function(d,i){ return height+50})
			.attr("r", 7)
			.style("fill", function(d){ return color(d)})

	svg.selectAll("mylabels")
		.data(keys)
		.enter()
		.append("text")
			.attr("x", function(d,i){ return 45 + i*100;})
			.attr("y", function(d,i){ return height+50;}) 
			.style("fill", function(d){ return color(d)})
			.text(function(d){ return d})
			.attr("text-anchor", "left")
			.style("alignment-baseline", "middle");


    svg.append("text")
		.attr("x", (width / 2))
		.attr("y", 0 - (margin.top / 2))
		.attr("text-anchor", "middle")
		.style("font-size", "16px")
		.text(title);
}

// Repeatable graph creation, HBAR
function createHBarGraph(divName, title, dataObject, leftKey, rightKey, widthScale = 1, heightScale = 1) {
	// Remove old SVGs
	const myNode = document.getElementById(divName.substring(1));
	while(myNode && myNode.firstChild){
		myNode.removeChild(myNode.lastChild);
	}

	//
    let margin = {top: 30, right: 30, bottom: 70, left: 60},
	width = (460*widthScale) - margin.left - margin.right,
	height = (400*heightScale) - margin.top - margin.bottom;

    const maxObs = dataObject.reduce(
    	function(acc, obj){
		return Math.max(obj[rightKey], acc);
	}, 0);

    // append the svg object to the body of the page
    let svg = d3.select(divName)
      .append("svg")
	.attr("width", width + margin.left + margin.right)
	.attr("height", height + margin.top + margin.bottom)
      .append("g")
	.attr("transform",
	      "translate(" + margin.left + "," + margin.top + ")");

    // x axis
    let x = d3.scaleBand()
	.range([0, width])
	.domain( dataObject.map(function(d) { return d[leftKey] }))
	.padding(0.2);
    svg.append("g")
		.attr("transform", "translate(0," + height + ")")
		.call(d3.axisBottom(x))
			.selectAll("text")
			.attr("transform", "translate(-10,0)rotate(-45)")
			.style("text-anchor", "end");

    // y axis
    let y = d3.scaleLinear()
      .domain([0, maxObs * 1.1])
      .range([ height, 0]);
    svg.append("g")
      .call(d3.axisLeft(y));

    // Bars

    svg.selectAll("mybar")
		.data(dataObject)
		.enter()
		.append("rect")
			.attr("x", function(d) { return x(d[leftKey]); })
			.attr("y", function(d) { return y(d[rightKey]); })
			.attr("width", x.bandwidth())
			.attr("height", function(d) { return height - y(d[rightKey]); })
			.attr("fill", "#69b3a2");

    svg.append("text")
		.attr("x", (width / 2))
		.attr("y", 0 - (margin.top / 2))
		.attr("text-anchor", "middle")
		.style("font-size", "16px")
		.text(title);
}

