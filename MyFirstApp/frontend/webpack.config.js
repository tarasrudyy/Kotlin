var webpack = require('webpack');

module.exports = {
    entry: "./entry.js",
    output: {
        path: __dirname + "/../public/compiled",
        filename: "bundle.js"
    },
    module: {
        loaders: [
            {
                test: /\.jsx?$/,
                loader: 'babel-loader',
                include: /frontend/,
                query: { presets: ['es2015', 'stage-0', 'react'] }
            },
            {
                test: /\.scss$/,
                loaders: ["style-loader", "css-loader", "sass-loader"]
            }
        ]
    }
};
