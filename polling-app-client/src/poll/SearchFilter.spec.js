import React from "react";
import { shallow } from "enzyme";
import { configure } from "enzyme";
import Adapter from "enzyme-adapter-react-16";
import SearchFilter from "./SearchFilter";

configure({ adapter: new Adapter() });

test('should render search Filter', () => {
  const searchFilter = shallow(<SearchFilter />);
  expect(searchFilter.find("form").length).toBe(1);
  expect(searchFilter.find("input").at(0).props().placeholder).toEqual(
    "Search by keyword"
  );
  expect(searchFilter.find('label').at(0).text()).toEqual('DatePicker');
  expect(searchFilter.find("button").at(0).text()).toEqual("Clear All");
  expect(searchFilter.find('button').at(1).text()).toEqual('Submit');
})

test("should change the selected radio button", () => {
    const searchFilter = shallow(<SearchFilter />);
    expect(searchFilter.find('r').props().placeholderText).toEqual('Select Date');
    searchFilter.setState({selectedOption : 'option2'})
    searchFilter.update();
    expect(searchFilter.find("r").props().placeholderText).toEqual("Select Range");
});
