import React from 'react'

import './TopNavigation.css'
import {navigateTo} from '../logic/location'
import Button from 'button';

const TopNavigation = React.createClass({
    propTypes: {
        items: React.PropTypes.arrayOf(React.PropTypes.object),
        oid: React.PropTypes.string
    },
    render: function() {
        const L = this.props.l10n;
        return (
            <div id="topNavigation">
                {this.props.items && this.props.items.backLocation
                    ? <Button href="#" action={this.changeViewAction(this.props.items.backLocation)}>&#8701; {L['TAKAISIN_LINKKI']}</Button>
                    : null}
                <ul className="tabs">{ this.props.items.map(this.item)}</ul>
            </div>
        )
    },
    
    item: function(value, idx, array) {
        let path = value.path;
        let key =  value.value;
        if(this.props.oid) {
            path += '?oid=' + this.props.oid;
        }
        const L = this.props.l10n;
        if (this.props.location.path === value.path || (!this.props.location.path && value.path === '/')) {
            return (<li key={idx} className="active">{L[key]}</li>);
        }
        return (<li key={idx} onClick={this.changeViewAction(path)}>{L[key]}</li>);
    },
    
    changeViewAction: function(to) {
        return () => {
            navigateTo(to);
        };
    }
});

export default TopNavigation;