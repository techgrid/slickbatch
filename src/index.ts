import dotenv from "dotenv";
import express from "express";
import path from "path";
import {AwsBatchMockService} from "./aws/batch/AwsBatchMockService";

dotenv.config();

const port = process.env.SERVER_PORT;

const app = express();

const service = new AwsBatchMockService();

app.get( "/", ( req, res ) => {
    res.send(service.submit(""));
});

app.listen( port, () => {
    // tslint:disable-next-line:no-console
    console.log( `server started at http://localhost:${ port }` );
} );
