const webpack = require('webpack');
const ExtractTextPlugin = require('extract-text-webpack-plugin');

module.exports = {
    entry: "./entry.js",
    output: {
        path: __dirname + "/../public/compiled",
        filename: "bundle.js"
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                loader: 'babel-loader',
                exclude: /node_modules/,
                query: { presets: ['es2015', 'stage-0', 'react'] }
            },
            {
                test: /\.scss$/,
                use: ExtractTextPlugin.extract({
                    fallback: 'style-loader',
                    use: ['css-loader', 'sass-loader']
                })
            }
        ]
    },
    plugins: [
        new ExtractTextPlugin("styles.css"),
    ]
};
