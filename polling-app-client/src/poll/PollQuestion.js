import React from 'react';
import { getAvatarColor } from '../util/Colors';
import { formatDateTime } from '../util/Helpers';
import { Avatar } from 'antd';
import { Link } from 'react-router-dom';
import './Poll.css';

function PollQuestion({poll}) {
    return (
        <div className="poll-header">
            <div className="poll-creator-info">
                <Link className="creator-link" to={`/users/${poll.createdBy.username}`}>
                    <Avatar className="poll-creator-avatar" 
                        style={{ backgroundColor: getAvatarColor(poll.createdBy.name)}} >
                        {poll.createdBy.name[0].toUpperCase()}
                    </Avatar>
                    <span className="poll-creator-name">
                        {poll.createdBy.name}
                    </span>
                    <span className="poll-creator-username">
                        @{poll.createdBy.username}
                    </span>
                    <span className="poll-creation-date">
                        {formatDateTime(poll.creationDateTime)}
                    </span>
                </Link>
            </div>
            <div className="poll-question">
                {poll.question}
            </div>
        </div>
    );
}

export default PollQuestion;