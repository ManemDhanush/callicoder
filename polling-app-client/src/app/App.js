import React, { Component } from 'react';
import './App.css';
import {
  Route,
  withRouter,
  Switch,
  Redirect
} from 'react-router-dom';

import { getCurrentUser } from '../util/APIUtils';
import { ACCESS_TOKEN } from '../constants';

import PollList from '../poll/PollList';
import NewPoll from '../poll/NewPoll';
import Login from '../user/login/Login';
import Signup from '../user/signup/Signup';
import Profile from '../user/profile/Profile';
import AppHeader from '../common/AppHeader';
import NotFound from '../common/NotFound';
import LoadingIndicator from '../common/LoadingIndicator';
import Dashboard from '../dashboard/Dashboard';
import PrivateRoute from '../common/PrivateRoute';
import { Layout, notification } from 'antd';
import Poll from '../poll/Poll';
const { Content } = Layout;

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      currentUser: null,
      isAuthenticated: false,
      isLoading: true,
      isAdmin: false,
      isPollster: false,
    };
    this.handleLogout = this.handleLogout.bind(this);
    this.loadCurrentUser = this.loadCurrentUser.bind(this);
    this.handleLogin = this.handleLogin.bind(this);

    notification.config({
      placement: 'topRight',
      top: 70,
      duration: 3,
    });    
  }

  loadCurrentUser() {
    getCurrentUser()
    .then(response => {
      this.setState({
        currentUser: response,
        isAuthenticated: true,
        isLoading: false,
        isAdmin: response.roles.includes('ROLE_ADMIN'),
        isPollster: response.roles.includes('ROLE_POLLSTER')
      });
    }).catch(error => {
      this.setState({
        isLoading: false
      });  
    });
  }

  componentDidMount() {
    this.loadCurrentUser();
  }

  handleLogout(redirectTo="/polls", notificationType="success", description="You're successfully logged out.") {
    localStorage.removeItem(ACCESS_TOKEN);

    this.setState({
      currentUser: null,
      isAuthenticated: false
    });

    this.props.history.push(redirectTo);
    
    notification[notificationType]({
      message: 'Polling App',
      description: description,
    });
  }

  handleLogin() {
    notification.success({
      message: 'Polling App',
      description: "You're successfully logged in.",
    });
    this.loadCurrentUser();
    this.props.history.push("/polls");
  }

  render() {
    if(this.state.isLoading) {
      return <LoadingIndicator />
    }
    
    return (
      <Layout className="app-container">
        <AppHeader
          isAuthenticated={this.state.isAuthenticated}
          currentUser={this.state.currentUser}
          onLogout={this.handleLogout}
        />

        {this.state.isAuthenticated && (
          <Dashboard
            isAuthenticated={this.state.isAuthenticated}
            currentUser={this.state.currentUser}
            isAdmin={this.state.isAdmin}
            isPollster={this.state.isPollster}
          />
        )}

        <Content className="app-content">
          <div className="container">
            <Switch>
              <Route exact path="/">
                <Redirect to="/polls" />
              </Route>
              <Route
                exact
                path="/polls"
                render={(props) => (
                  <PollList
                    isAuthenticated={this.state.isAuthenticated}
                    currentUser={this.state.currentUser}
                    handleLogout={this.handleLogout}
                    {...props}
                  />
                )}
              ></Route>
              <Route
                path="/login"
                render={(props) => (
                  <Login onLogin={this.handleLogin} {...props} />
                )}
              ></Route>
              <Route path="/signup" component={Signup}></Route>
              <Route
                path="/users/:username"
                render={(props) => (
                  <Profile
                    isAuthenticated={this.state.isAuthenticated}
                    currentUser={this.state.currentUser}
                    {...props}
                  />
                )}
              ></Route>
              <PrivateRoute
                authenticated={
                  this.state.isAuthenticated &&
                  (this.state.isAdmin || this.state.isPollster)
                }
                path="/poll/new"
                component={NewPoll}
                handleLogout={this.handleLogout}
              ></PrivateRoute>
              <PrivateRoute
                authenticated={this.state.isAuthenticated}
                path="/poll/:pollid"
                component={Poll}
                handleLogout={this.handleLogout}
              ></PrivateRoute>
              <Route component={NotFound}></Route>
            </Switch>
          </div>
        </Content>
      </Layout>
    );
  }
}

export default withRouter(App);
