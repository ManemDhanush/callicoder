import React, { useEffect, useState } from "react";
import { Layout, Progress } from "antd";
import "./Dashboard.css";
import { InactivePollsCount } from "../util/Helpers";
import {
  getAllPolls,
  getUserCreatedPolls,
  getUserVotedPolls,
} from "../util/APIUtils";

const Sider = Layout.Sider;

const Dashboard = (props) => {
  const [pollInfo, setPollInfo] = useState({
    totalPolls: 0,
    activePolls: 0
  });
  const [userCreatedInfo, setUserCreatedInfo] = useState({
    userTotalPolls: 0,
    userInactivePolls: 0,
  });
  const [userVotedInfo, setUserVotedInfo] = useState({
    userVotedPolls: 0
  });

  useEffect(() => {
    handleData();
  }, []);

  const handleData = () => {
    getAllPolls().then((res) => {
      setPollInfo({
        totalPolls: res.content.length,
        activePolls: res.content.length - InactivePollsCount(res.content),
      });
    });
    getUserCreatedPolls(props.currentUser.username).then((res) => {
      setUserCreatedInfo({
        userTotalPolls: res.content.length,
        userInactivePolls: InactivePollsCount(res.content),
      });
    });
    getUserVotedPolls(props.currentUser.username).then((res) => {
      setUserVotedInfo({
        userVotedPolls: res.content.length
      });
    });
  };

  const card = (desc, title, progressPercent) => (
    <div className="card">
      <div className="desc">{title}: {desc}</div>
      {progressPercent && (
        <Progress status="normal" type="circle" percent={progressPercent} />
      )}
    </div>
  );

  const userVotedPollCard = card(
    userVotedInfo.userVotedPolls,
    "Number of polls you voted",
    userVotedInfo.userVotedPolls * (100 / pollInfo.totalPolls).toFixed(1)
  );

  const userCreatedPollCard = (
    <div className="card">
      <div className="desc">Number of polls created by you</div>
      <h1 className="title">{userCreatedInfo.userTotalPolls}</h1>
    </div>
  );

  const totalPollCard = (
    <div className="card">
      <div className="desc">
        The total number of polls till{" "}
        {new Date().toLocaleString().substring(0, 10)}
      </div>
      <h1 className="title">{pollInfo.totalPolls}</h1>
    </div>
  );

  const activePollCard = card(
    pollInfo.activePolls,
    "Currently active polls",
    pollInfo.activePolls * (100 / pollInfo.totalPolls).toFixed(1)
  );

  const userInactivePollCard = card(
    userCreatedInfo.userInactivePolls,
    "Your Inactive polls",
    userCreatedInfo.userInactivePolls * (100 / userCreatedInfo.userTotalPolls).toFixed(1)
  );

  const votedPollCard = card(
    userVotedInfo.userVotedPolls,
    "Number of polls you voted",
    userVotedInfo.userVotedPolls * (100 / pollInfo.totalPolls).toFixed(1)
  );

  return (
    <Sider className="app-sider">
      {props.isAdmin && totalPollCard}

      {props.isPollster && userCreatedPollCard}

      {!props.isPollster && !props.isAdmin && userVotedPollCard}

      {props.isAdmin && activePollCard}

      {props.isPollster && userInactivePollCard}

      {props.isPollster && votedPollCard}
    </Sider>
  );
};

export default Dashboard;
