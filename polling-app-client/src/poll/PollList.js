import React, { useEffect, useState } from 'react';
import { getAllPolls, getUserCreatedPolls, getUserVotedPolls, filterPolls } from '../util/APIUtils';
import LoadingIndicator  from '../common/LoadingIndicator';
import { Card, Pagination } from 'antd';
import { POLL_LIST_SIZE } from '../constants';
import { withRouter, Link } from 'react-router-dom';
import SearchFilter from './SearchFilter';
import PollQuestion from './PollQuestion';
import './PollList.css';

function PollList (props) {
    const [polls, setPolls] = useState([]);
    const [page, setPage] = useState(1);
    const [totalElements, setTotalElements] = useState(0);
    const [isLoading, setIsLoading] = useState(false);
    const [filters, setFilters] = useState("");

    const loadPollList = () => {
        let promise;
        if(props.username) {
            if(props.type === 'USER_CREATED_POLLS') {
                promise = getUserCreatedPolls(props.username, page, POLL_LIST_SIZE);
            } else if (props.type === 'USER_VOTED_POLLS') {
                promise = getUserVotedPolls(props.username, page, POLL_LIST_SIZE);                               
            }
        } else {
            promise = filters ? filterPolls(filters, page -1, POLL_LIST_SIZE) : getAllPolls(page - 1, POLL_LIST_SIZE);
        }

        if(!promise) {
            return;
        }
        setIsLoading(true);
        promise            
        .then(response => {
            setPolls(response.content);
            setTotalElements(response.totalElements);
        }).finally(() => setIsLoading(false));
    }

    useEffect(() => {
        loadPollList();
    }, []);

    useEffect(() => {
        if(props.isAuthenticated) {
            reset();
            loadPollList();
        } 
    }, [props.isAuthenticated]);

    useEffect(() => {
        loadPollList();
    }, [page, filters])

    const reset = () => {
        setPolls([]);
        setPage(1);
        setTotalElements(0);
        setIsLoading(false);
        setFilters("");
    }

    return (
        <div className='polls-list'>
            <div className='polls-filter'>
                <span className='heading'>Search Filter</span>
                <SearchFilter onLoad={loadPollList} reset={reset} setFilters={setFilters} />
            </div>
            <div className="polls-container">
                {
                    isLoading ? 
                    <LoadingIndicator />: null
                }
                <div className='poll-questions'>
                { 
                    !isLoading && polls && polls.map((poll) => {
                    return(
                        <Link to={`/poll/${poll.id}`}>
                            <Card className='polls'>
                                <PollQuestion poll={poll} />
                            </Card>
                        </Link>
                    )})
                }
                </div>
                {
                    !isLoading && polls && polls.length === 0 ? (
                        <div className="no-polls-found">
                            <span>No Polls Found.</span>
                        </div>    
                    ): null
                }
                <div className='pagination'>
                    <Pagination
                        current={page} 
                        total={totalElements} 
                        pageSize={POLL_LIST_SIZE} 
                        onChange={page => setPage(page)} 
                    />
                </div>
            </div>
        </div>
    );
}

export default withRouter(PollList);