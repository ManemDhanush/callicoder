import React from 'react';
import SignUp from './Signup';
import {shallow} from 'enzyme';
import { configure } from "enzyme";
import Adapter from "enzyme-adapter-react-16";

configure({ adapter: new Adapter() });
const signupWrapper = shallow(<SignUp/>);

test('Sign Up page should have form item to pick user role',()=>{
  expect(signupWrapper.find("FormItem").length).toBe(6);
  expect(signupWrapper.find('FormItem').at(4).prop('label')).toBe("Role for User");

})

test("Pick user role should be provided choice of 3 roles", () => {
  const checkBox = signupWrapper.find("FormItem").at(4).find("CheckboxGroup");

  expect(checkBox.prop("options").length).toBe(3);
  expect(checkBox.prop("options")).toEqual([
    { label: "User", value: "ROLE_USER", disabled: true },
    { label: "Pollster", value: "ROLE_POLLSTER" },
    { label: "Admin", value: "ROLE_ADMIN" },
  ]);
});


test('should raise an error when password length is less than 6 characters', () => {
    const event = { 
        target : {value: "pass" , name: "password"}
    };
    signupWrapper.find('Password').props().onChange(event);
    signupWrapper.update();
    expect(signupWrapper.find('FormItem').at(3).props().validateStatus).toEqual('error');
    expect(signupWrapper.find("FormItem").at(3).props().help)
    .toEqual('Password is too short (Minimum 6 characters needed.)');
    expect(signupWrapper.find('span').props()['data-score']).toEqual('error-0');
})

test('should raise an error when password length is more than 20', () =>{
    const event = {
    target: { value: "passworddddddddddddd11", name: "password" },
    };
    signupWrapper.find("Password").props().onChange(event);
    signupWrapper.update();
    expect(signupWrapper.find("FormItem").at(3).props().validateStatus).toEqual("error");
    expect(signupWrapper.find("FormItem").at(3).props().help).toEqual(
      "Password is too long (Maximum 20 characters allowed.)"
    );
    expect(signupWrapper.find("span").props()["data-score"]).toEqual("error-3");
})

test('should raise an error when password does not contain the required characters', () =>{
    const event = {
    target: { value: "Check123", name: "password" },
    };
    signupWrapper.find("Password").props().onChange(event);
    signupWrapper.update();
    expect(signupWrapper.find("FormItem").at(3).props().validateStatus).toEqual("error");
    expect(signupWrapper.find("FormItem").at(3).props().help).toEqual(
      "Password must contain uppercase, lowercase, digits and special characters"
    );
    expect(signupWrapper.find("span").props()["data-score"]).toEqual("error-1");
})

test('should accept the password entered', () => {
    const event = {
    target: { value: "Check@1234", name: "password" },
    };
    signupWrapper.find("Password").props().onChange(event);
    signupWrapper.update();
    expect(signupWrapper.find("FormItem").at(3).props().validateStatus).toEqual("success");
    expect(signupWrapper.find("FormItem").at(3).props().help).toEqual(null);
    expect(signupWrapper.find("span").props()["data-score"]).toEqual(3);
})
