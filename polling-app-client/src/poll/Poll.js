import React, { Component } from 'react';
import './Poll.css';
import { Icon } from 'antd';
import { withRouter } from 'react-router-dom';
import { getPollById, castVote } from '../util/APIUtils';

import LoadingIndicator from '../common/LoadingIndicator';
import PollQuestion from './PollQuestion';
import { Radio, Button, notification } from 'antd';
import NotFound from '../common/NotFound';
const RadioGroup = Radio.Group;

class Poll extends Component {
    constructor(props) {
        super(props);
        this.state= {
            poll: undefined,
            isLoading: true,
            currentChoice: null,
            timeRemaining: ""
        }
        this.loadPoll = this.loadPoll.bind(this);
        this.handleVoteChange = this.handleVoteChange.bind(this);
        this.handleVoteSubmit = this.handleVoteSubmit.bind(this);
        this.getTimeRemaining = this.getTimeRemaining.bind(this);
    }

    componentDidMount() {
        this.loadPoll();
    }

    loadPoll = () => {
        const {pathname} = this.props.location;
        const path = pathname.split('/');
        const promise = getPollById(path[path.length-1]);
        if(!promise) {
            return;
        }

        this.setState({
            isLoading: true
        });

        promise.then(response => {
            this.setState({
              poll: response,
              isLoading: false,
              timeRemaining: this.getTimeRemaining(response),
              currentChoice: response
                ? response.selectedChoice
                : null,
            });
        }).catch(error => {
            this.setState({
                isLoading: false
            })
        });
    }

    handleVoteChange(event) {
        this.setState({
            currentChoice: event.target.value
        });
    }

    handleVoteSubmit(event) {
        event.preventDefault();
        const selectedChoice = this.state.currentChoice;
        const voteData = {
            pollId: this.state.poll.id,
            choiceId: selectedChoice
        };
        castVote(voteData)
        .then(response => {
            this.setState({
                poll: response
            });        
        }).catch(error => {
            if(error.status === 401) {
                this.props.handleLogout('/login', 'error', 'You have been logged out. Please login to vote');    
            } else {
                notification.error({
                    message: 'Polling App',
                    description: error.message || 'Sorry! Something went wrong. Please try again!'
                });                
            }
        });
    }

    calculatePercentage = (choice) => {
        if(this.state.poll.totalVotes === 0) {
            return 0;
        }
        return (choice.voteCount*100)/(this.state.poll.totalVotes);
    };

    isSelected = (choice) => {
        return this.state.poll.selectedChoice === choice.id;
    }

    getWinningChoice = () => {
        return this.state.poll.choices.reduce((prevChoice, currentChoice) => 
            currentChoice.voteCount > prevChoice.voteCount ? currentChoice : prevChoice, 
            {voteCount: -Infinity}
        );
    }

    getTimeRemaining = (poll) => {
        const expirationTime = new Date(poll.expirationDateTime).getTime();
        const currentTime = new Date().getTime();
    
        var difference_ms = expirationTime - currentTime;
        var minutes = Math.floor( (difference_ms/1000/60) % 60 );
        var hours = Math.floor( (difference_ms/(1000*60*60)) % 24 );
        var days = Math.floor( difference_ms/(1000*60*60*24) );
    
        let timeRemaining = days + " days " + hours + " hours " + minutes + " minutes left ";
        
        return timeRemaining;
    }

    render() {
        if(this.state.isLoading) {
            return <LoadingIndicator />
        }
        const pollChoices = [];
        setInterval(() => {
            this.setState({
              timeRemaining: this.getTimeRemaining(this.state.poll),
            });
        }, 60000);

        if(this.state.poll === undefined) {
            return <NotFound />
        }

        if(this.state.poll.selectedChoice || this.state.poll.expired) {
            const winningChoice = this.state.poll.expired ? this.getWinningChoice() : null;

            this.state.poll.choices.forEach(choice => {
                pollChoices.push(<CompletedOrVotedPollChoice 
                    key={choice.id} 
                    choice={choice}
                    isWinner={winningChoice && choice.id === winningChoice.id}
                    isSelected={this.isSelected(choice)}
                    percentVote={this.calculatePercentage(choice)}
                />);
            });                
        } else {
            this.state.poll.choices.forEach(choice => {
                pollChoices.push(<Radio className="poll-choice-radio" key={choice.id} value={choice.id}>{choice.text}</Radio>)
            })    
        }        
        return (
            <div className="poll-content">
                <PollQuestion poll={this.state.poll} />
                <div className="poll-choices">
                    <RadioGroup 
                        className="poll-choice-radio-group" 
                        onChange={this.handleVoteChange} 
                        value={this.state.currentChoice}>
                        { pollChoices }
                    </RadioGroup>
                </div>
                <div className="poll-footer">
                    { 
                        !(this.state.poll.selectedChoice || this.state.poll.expired) ?
                        (<Button className="vote-button" disabled={!this.state.currentChoice} onClick={this.handleVoteSubmit}>Vote</Button>) : null 
                    }
                    <span className="total-votes">{this.state.poll.totalVotes} votes</span>
                    <span className="separator">•</span>
                    <span className="time-left">
                        {
                            this.state.poll.expired ? "Final results" : this.state.timeRemaining
                        }
                    </span>
                </div>
            </div>
        );
    }
}

function CompletedOrVotedPollChoice(props) {
    return (
        <div className="cv-poll-choice">
            <span className="cv-poll-choice-details">
                <span className="cv-choice-percentage">
                    {Math.round(props.percentVote * 100) / 100}%
                </span>            
                <span className="cv-choice-text">
                    {props.choice.text}
                </span>
                {
                    props.isSelected ? (
                    <Icon
                        className="selected-choice-icon"
                        type="check-circle-o"
                    /> ): null
                }    
            </span>
            <span className={props.isWinner ? 'cv-choice-percent-chart winner': 'cv-choice-percent-chart'} 
                style={{width: props.percentVote + '%' }}>
            </span>
        </div>
    );
}

export default withRouter(Poll);