import React from "react";
import { shallow } from "enzyme";
import { configure } from "enzyme";
import Adapter from "enzyme-adapter-react-16";
import { BrowserRouter as Router } from 'react-router-dom';
import PollList from "./PollList";

configure({ adapter: new Adapter() });

test('should render pagination', () => {
  const wrapper = shallow( 
    <Router>
      <PollList currentUser={{roles:["ROLE_ROLE"]}} /> 
    </Router>
  ).dive().dive().dive().dive();
  expect(wrapper.find('Pagination').length).toBe(1);
});