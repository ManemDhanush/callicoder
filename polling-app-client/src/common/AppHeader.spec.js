import React from 'react';
import AppHeader from './AppHeader';
import {shallow} from 'enzyme';
import { MemoryRouter } from 'react-router-dom';
import { configure } from "enzyme";
import Adapter from "enzyme-adapter-react-16";

configure({ adapter: new Adapter() });

test('Pollster role should have menu item to create new poll',()=>{
  const wrapper=shallow(
    <MemoryRouter>
      <AppHeader currentUser={{roles:["ROLE_POLLSTER"]}}/>
    </MemoryRouter>
  ).dive().dive().dive().dive()
  const menuItems=wrapper.find('MenuItem')

  expect(menuItems.length).toBe(3);
  expect(menuItems.at(1).find('Link').prop('to')).toBe('/poll/new');
})

test("User role should not have menu item to create new poll", () => {
  const wrapper = shallow(
    <MemoryRouter>
      <AppHeader currentUser={{ roles: ["ROLE_USER"] }} />
    </MemoryRouter>
  ).dive().dive().dive().dive();

  expect(wrapper.find("MenuItem").length).toBe(2);
  expect(wrapper.find("MenuItem").at(1).find("Link").length).toBe(0);
});