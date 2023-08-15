import React, { Component } from "react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.min.css";
import './SearchFilter.css';

const defaultState = {
  keyword: "",
  date: "",
  startDate: "",
  endDate: "",
  selectedOption: "option1",
};

class SearchFilter extends Component {
  constructor(props) {
    super(props);
    this.state = defaultState;

    this.handleKeywordChange = this.handleKeywordChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
    this.handleClear = this.handleClear.bind(this);
    this.handleOptionChange = this.handleOptionChange.bind(this);
    this.handleDateRangeChange = this.handleDateRangeChange.bind(this);
  }

  handleKeywordChange(event) {
    this.setState({
      keyword : event.target.value,
    })
  }

  handleSubmit (event) {
    event.preventDefault();
    this.props.reset();
    this.props.setFilters(this.generateFilterUrl());
  }

  handleClear () {
    const { keyword, date, startDate, endDate } = this.state;
    if(keyword === '' && date === '' && startDate === '' && endDate === '') return;
    this.setState(defaultState);
    this.props.reset();
  }

  handleDateRangeChange = (range) => {
    const [startDate, endDate] = range;
    this.setState({
      startDate : startDate,
      endDate : endDate
    })
  }

  handleOptionChange(event) {
    this.setState({
      selectedOption : event.target.value
    }, () => {
      if(this.state.selectedOption === 'option1')
      {
        this.setState({
          startDate : "",
          endDate : ""
        }) 
      } else {
        this.setState({date : ""})
      }
    })
  }

  dateToString (date) {
    return date.toLocaleDateString("fr-CA");
  }

  generateFilterUrl() {
    let filterObject = {
      date: this.state.date ? this.dateToString(this.state.date) : "",
      keywords: this.state.keyword,
      startDate: this.state.startDate ? this.dateToString(this.state.startDate) : "",
      endDate: this.state.endDate ? this.dateToString(this.state.endDate) : ""
    };
    const stringfyObj = JSON.stringify(filterObject, (key, value) => {
      if(!value)
        return undefined;
      return value;
    });

    filterObject = JSON.parse(stringfyObj);
    const queryString = Object.entries(filterObject).map(([key, value]) => `${key}=${encodeURIComponent(value)}`).join('&')
    return queryString;
  }

  render() {
    return (
      <form className="filter-form" onSubmit={this.handleSubmit}>
        <button onClick={this.handleClear}>Clear All</button>
        <input
          type="text"
          data-testid="keyword"
          placeholder="Search by keyword"
          value={this.state.keyword}
          onChange={this.handleKeywordChange}
          className="filter-keyword"
        />
          <div className="radio">
            <label className="form-control">
              <input
                type="radio"
                value="option1"
                checked={this.state.selectedOption === "option1"}
                onChange={this.handleOptionChange}
                className="radio-button"
              />
              DatePicker
            </label>
            <label className="form-control">
              <input
                type="radio"
                value="option2"
                checked={this.state.selectedOption === "option2"}
                onChange={this.handleOptionChange}
                className="radio-button"
              />
              DateRangePicker
            </label>
          </div>
        {this.state.selectedOption === "option1" ? (
          <DatePicker
            selected={this.state.date}
            onChange={(date) => this.setState({ date: date })}
            placeholderText="Select Date"
          />
        ) : (
          <DatePicker
            selected={this.state.startDate}
            onChange={this.handleDateRangeChange}
            startDate={this.state.startDate}
            endDate={this.state.endDate}
            placeholderText="Select Range"
            selectsRange
          />
        )}
        <button type="submit" className="submit">
          Submit
        </button>
      </form>
    );
  }
}

export default SearchFilter;
