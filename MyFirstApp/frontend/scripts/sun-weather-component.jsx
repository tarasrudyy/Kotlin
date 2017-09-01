import React from 'react';
import axios from 'axios';

class SunWeatherComponent extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            sunrise: undefined,
            sunset: undefined,
            temperature: undefined
        };
    }
    componentDidMount = () => {
        axios.get('/api/data').then((response) => {
            const json = response.data;
            this.setState({
                sunrise: json.sunInfo.sunrise,
                sunset: json.sunInfo.sunset,
                temperature: json.temperature
            });
        });
    }
    render = () => {
        return <div>
            <div>Sunrise time: {this.state.sunrise}</div>
            <div>Sunset time: {this.state.sunset}</div>
            <div>Current temperature: {this.state.temperature}</div>
        </div>
    }
}

export default SunWeatherComponent;
